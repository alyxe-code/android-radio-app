package com.p2lem8dev.internetRadio.app.service.sync.extractors

import android.webkit.URLUtil
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

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
        if (connection.responseCode != HttpURLConnection.HTTP_OK)
            return null

        // filter m3u file strings to find valid urls
        var urls = url.readText()
            .split("\n")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.isNotBlank() }
            .filter { URLUtil.isValidUrl(it) && (URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it)) }
            .toList()

        // select only running servers
        if (onlyRunning) {
            urls = urls.filter {
                try {
                    val connectionForFound = getConnection(it)
                    connectionForFound != null && connectionForFound.responseCode == HttpURLConnection.HTTP_OK
                } catch (e: Exception) {
                    false
                }
            }
        }

        return when {
            urls.size > 1 -> urls
            urls.isEmpty() -> null
            else -> urls
        }
    }

}