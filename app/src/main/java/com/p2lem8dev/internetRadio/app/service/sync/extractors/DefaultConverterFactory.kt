package com.p2lem8dev.internetRadio.app.service.sync.extractors

import android.util.Log
import android.webkit.URLUtil
import com.p2lem8dev.internetRadio.net.utils.InternetConnectionTester
import java.net.HttpURLConnection
import java.net.URL

class DefaultConverterFactory : ConverterFactory {

    override fun extract(
        urlString: String,
        onlyRunning: Boolean,
        getConnection: (String?) -> HttpURLConnection?
    ): List<String>? {
        try {
            InternetConnectionTester.waitConnection()
            // check that URL is not returning error code
            if (URLConverterFactory.isError(urlString)) {
                return null
            }
            val url = URL(urlString)

            // validate content size
            try {
                val contentLength = url.openConnection().contentLength
                if (contentLength != -1 && contentLength / 1024 > 10) {
                    Log.d("SYNC_TEST", "CONTENT_SIZE is too high")
                    return null
                }
            } catch (e: Exception) {
                // ignore content length
            }

            // content content from URL
            val contentText = readURLContent(url) ?: return null

            // search for URLs in content
            var foundURLs = arrayListOf<String>()
            var foundURL = ConverterFactory.URL_REGEX.find(contentText)
            while (true) {
                if (foundURL?.value == null || foundURL.value == "null") break
                if (URLUtil.isValidUrl(foundURL.value) &&
                    foundURL.value.subSequence(0, 4) == "http"
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
                foundURLs = foundURLs.filter { URLConverterFactory.isRunning(it) }
                        as ArrayList<String>
            }

            return if (foundURLs.isEmpty()) null
            else foundURLs
        } catch (e: Exception) {
            return null
        }
    }

    companion object {
        // Max number of seconds to read content from URL
        const val MAX_READ_TIME = 2000L
    }
}