package com.p2lem8dev.internetRadio.app.service.sync.extractors

import android.util.Log
import android.webkit.URLUtil
import com.p2lem8dev.internetRadio.net.utils.InternetConnectionTester
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Extract urls from audio/x-mpegurl
 */
class M3UConverterFactory : ConverterFactory {
    override fun getSupportedContentTypes(): List<String>? {
        return listOf("audio/x-mpegurl")
    }

    override fun getSupportedFileExtensions(): List<String>? {
        return listOf("m3u", "m3u8")
    }

    override fun extract(
        urlString: String,
        onlyRunning: Boolean,
        getConnection: (String?) -> HttpURLConnection?
    ): List<String>? {
        val url: URL
        try {
            url = URL(urlString)
        } catch (e: Exception) {
            return null
        }

        val connection = url.openConnection() as HttpURLConnection
        while (true) {
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK)
                    return null
                break
            } catch (e: Exception) {
                InternetConnectionTester.waitConnection()
            }
        }

        // filter m3u file strings to find valid urls
        val text = readURLContent(url) ?: return null

        var urls = text
            .split("\n")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.isNotBlank() }
            .filter { URLUtil.isValidUrl(it) && (URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it)) }
            .toList()

        // select only running servers
        if (onlyRunning) {
            urls = urls.filter { URLConverterFactory.isRunning(it) }
        }

        return when {
            urls.size > 1 -> urls
            urls.isEmpty() -> null
            else -> urls
        }
    }

}