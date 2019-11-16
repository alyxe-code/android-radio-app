package com.p2lem8dev.internetRadio.app.service.sync.extractors

import android.util.Log
import android.webkit.URLUtil
import com.p2lem8dev.internetRadio.net.utils.InternetConnectionTester
import java.net.HttpURLConnection
import java.net.URL

/**
 * URLConverterFactory
 * This is a simple converter class to extract all URls from original one.
 * @author Alex Repin alexander.repin@arcadia.spb.ru
 */
class URLConverterFactory private constructor(
    private var defaultConverterFactory: ConverterFactory? = null,
    private var urlString: String
) {

    /**
     * Allow or refuse redirection on getting connected
     */
    private var isFollowRedirection: Boolean = false

    /**
     * List of converters
     */
    private val converters = arrayListOf<ConverterFactory>()

    /**
     * Some added to API stations are not available
     * at the given time. When false these stations' links
     * will be ignored
     */
    private var extractOnlyRunning = false

    /**
     * Get list of all available extractors by file extension
     */
    private fun findExtractorsByExtension(extension: String) = converters.filter {
        it.getSupportedFileExtensions()?.contains(extension) ?: false
    }

    /**
     * Get list of all available extractors by file type (content type)
     */
    private fun findExtractorsByFormat(format: String) = converters.filter {
        it.getSupportedContentTypes()?.contains(format) ?: false
    }

    /**
     * When passed url is valid return it otherwise
     * try to extract urls from content of the url
     */
    fun extract(): List<String>? {
        // connect to current URL
        val connection = getConnection(urlString) ?: return null

        InternetConnectionTester.waitConnection()
        var responseCode: Int

        while (true) {
            try {
                InternetConnectionTester.waitConnection()
                responseCode = connection.responseCode
                break
            } catch (e: Exception) {
            }
        }

        // check whether response code is error
        if (isError(urlString)) {
            Log.d("SYNC_TEST", "$urlString returned $responseCode")
            return null
        }
        // check whether it's possible to continue
        if (isRedirect(urlString) && !isFollowRedirection) {
            Log.d("SYNC_TEST", "Cannot continue | STATUS_CODE: $responseCode")
            return null
        }
        // sometimes Content-Type may be null then just skip this extraction
        if (connection.contentType != null) {
            // clear content type
            InternetConnectionTester.waitConnection()
            val contentType = connection.contentType.split(";")[0]
            // check if the url is valid
            if (SUPPORTED_FINAL_AUDIO_FORMATS.find { it == contentType } != null) {
                Log.d("SYNC_TEST", "Passed URL is valid")
                return listOf(urlString)
            }
            // try to extract by content type
            for (extractor in findExtractorsByFormat(contentType)) {
                val extracted =
                    extractor.extract(urlString, extractOnlyRunning) { getConnection(it) }
                if (extracted == null || extracted.isEmpty()) continue
                Log.v("SYNC_TEST", "|| Extracted by content-type")
                return extracted
            }
        }
        // try to extract by file extension
        val fileExt = urlString.substring(urlString.lastIndexOf('.') + 1)
        for (extractor in findExtractorsByExtension(fileExt)) {
            val extracted = extractor.extract(urlString, extractOnlyRunning) { getConnection(it) }
            if (extracted == null || extracted.isEmpty()) continue
            Log.v("SYNC_TEST", "|| Extracted by file extension")
            return extracted
        }
        // when job is not done try to get default converter factory
        if (defaultConverterFactory == null) {
            defaultConverterFactory = ConverterFactory.getDefaultExtractor()
        }
        // handle extraction with default converter factory
        val extract = defaultConverterFactory
            ?.extract(urlString, extractOnlyRunning)
            { getConnection(it) }
        return if (extract == null) {
            Log.w("SYNC_TEST", "|| Default extractor failed")
            null
        } else {
            Log.v("SYNC_TEST", "|| Default extractor succeeded")
            extract
        }
    }

    /**
     * Get a valid connection after following redirection line (if allowed)
     */
    private fun getConnection(urlString: String?): HttpURLConnection? {
        if (urlString == null) return null
        val clearURLString = urlString
            .trim()
            .replace("\t", "")
            .replace("\n", "")
        if (!URLUtil.isValidUrl(clearURLString)) {
            return null
        }
        return try {
            InternetConnectionTester.waitConnection()
            var connection = URL(clearURLString).openConnection() as HttpURLConnection
            if (connection.responseCode.toString()[0] == '3' && isFollowRedirection) {
                val url = followRedirection(URL(clearURLString)) ?: return null
                connection = (url.openConnection() as HttpURLConnection).apply { connect() }
            }
            connection
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Follow redirection line until it's over
     */
    private fun followRedirection(url: URL?): URL? {
        if (url == null) return null
        InternetConnectionTester.waitConnection()
        val nextRegexResult = URL_REGEX.find(url.readText())
        return if (nextRegexResult == null) null
        else {
            var redirectURL: URL? = URL(nextRegexResult.value)
            try {
                InternetConnectionTester.waitConnection()
                if (isRedirect(redirectURL.toString())) {
                    redirectURL = followRedirection(redirectURL)
                }
                redirectURL
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        /**
         * Some URLs are not final endpoints but just a transport
         * between radio api and the web. This extracts such URLs
         * when it's necessary because some URLs are already valid
         * @param urlString The origin endpoint for fetching
         * @param extractOnlyRunning Only live urls will be extracted
         * @param converters List of advanced converters
         * @param followRedirection Allow to follow redirection line fetching
         * @param defaultConverterFactory The default converter factory. Usually used the DefaultConverterFactory
         * @return List of fetched stations' links
         */
        fun extract(
            urlString: String,
            extractOnlyRunning: Boolean = true,
            converters: List<ConverterFactory>? = null,
            followRedirection: Boolean = true,
            defaultConverterFactory: ConverterFactory? = null
        ): List<String>? {
            return URLConverterFactory(defaultConverterFactory, urlString).apply {
                isFollowRedirection = followRedirection
                this.extractOnlyRunning = extractOnlyRunning
                if (converters != null) {
                    this.converters.addAll(converters)
                }
            }.extract()
        }

        val URL_REGEX =
            Regex("(http|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?")

        val SUPPORTED_FINAL_AUDIO_FORMATS = listOf("audio/mpeg")

        private fun getResponseCodeOf(url: URL): Int {
            val connection = url.openConnection() as HttpURLConnection

            var responseCode: Int
            while (true) {
                try {
                    responseCode = connection.responseCode
                    break
                } catch (e: Exception) {
                    InternetConnectionTester.waitConnection()
                }
            }

            return responseCode
        }

        fun isRunning(url: URL) = try {
            getResponseCodeOf(url) == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }

        fun isRunning(urlString: String) = try {
            getResponseCodeOf(URL(urlString)) == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }

        fun isRedirect(url: URL) = try {
            getResponseCodeOf(url).toString()[0] == '3'
        } catch (e: Exception) {
            false
        }

        fun isRedirect(urlString: String) = try {
            getResponseCodeOf(URL(urlString)).toString()[0] == '3'
        } catch (e: Exception) {
            false
        }

        private fun checkResponseCodeIsError(responseCode: Int): Boolean {
            val codeFirstChar = responseCode.toString()[0]
            return codeFirstChar != '2' && codeFirstChar != '3'
        }

        fun isError(urlString: String) = try {
            InternetConnectionTester.waitConnection()
            checkResponseCodeIsError(
                (URL(urlString).openConnection() as HttpURLConnection)
                    .responseCode
            )
        } catch (e: Exception) {
            true
        }

    }
}