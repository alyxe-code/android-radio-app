package com.p2lem8dev.internetRadio.app.service.sync.extractors

import android.util.Log
import android.webkit.URLUtil
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL

class DefaultConverterFactory : ConverterFactory {

    private fun readFileText(url: URL): String? {
        var result: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(url.openStream()))
            var inputLine: String?
            val stringBuilder = StringBuilder()

            val start = System.currentTimeMillis()
            while (true) {
                inputLine = reader.readLine()
                if (inputLine == null || inputLine == "null") break
                stringBuilder.append(inputLine)
                val currentTime = System.currentTimeMillis()
                if (currentTime - start > MAX_READ_TIME) break
            }

            result = stringBuilder.toString()
        } catch (e: Exception) {
            Log.d(
                "SYNC_TEST",
                "DEFAULT_EXTRACTOR |" +
                        " Read from url by BufferReader thrown exception ${e.message}\n" +
                        e.stackTrace.joinToString("\n")
            )
        }
        Log.d("SYNC_TEST", "READ: $result")
        return result
    }

    override fun extract(
        urlString: String,
        onlyRunning: Boolean,
        getConnection: (String?) -> HttpURLConnection?
    ): List<String>? {
        try {
            // check that URL is not returning error code
            if (URLConverterFactory.isError(urlString)) {
                return null
            }
            val url = URL(urlString)
            val findUrlsRegex =
                Regex("(http|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?")

            // validate content size
            val contentLength = url.openConnection().contentLength
            if (contentLength != -1 && contentLength / 1024 > 10) {
                Log.d("SYNC_TEST", "CONTENT_SIZE is too high")
                return null
            }

            // content content from URL
            val contentText = readFileText(url) ?: return null

            // search for URLs in content
            var foundURLs = arrayListOf<String>()
            var foundURL = findUrlsRegex.find(contentText)
            while (true) {
                if (foundURL?.value == null || foundURL.value == "null") break
                if (URLUtil.isValidUrl(foundURL.value) && foundURL.value.subSequence(
                        0,
                        4
                    ) == "http"
                ) {
                    foundURLs.add(foundURL.value)
                }
                foundURL = foundURL.next()
            }

            if (foundURLs.isEmpty()) {
                Log.d("SYNC_TEST", "DEFAULT_EXTRACTOR | No URL found")
                return null
            }

            // filter by running | if necessary
            if (onlyRunning) {
                foundURLs = foundURLs.filter {
                    try {
                        val connection = getConnection(it)
                        connection != null && connection.responseCode != HttpURLConnection.HTTP_OK &&
                                connection.contentType == "audio/mpeg"
                    } catch (e: Exception) {
                        false
                    }
                } as ArrayList<String>
            }

            return foundURLs
        } catch (e: Exception) {
            return null
        }
    }

    companion object {
        // Max number of seconds to read content from URL
        const val MAX_READ_TIME = 2000L
    }
}