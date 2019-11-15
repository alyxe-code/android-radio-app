package com.p2lem8dev.internetRadio.app.service.sync.extractors

import android.annotation.SuppressLint
import android.webkit.URLUtil
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class PLSConverterFactory : ConverterFactory {
    override fun getSupportedFileExtensions(): List<String>? {
        return listOf("pls")
    }

    @SuppressLint("DefaultLocale")
    override fun extract(
        urlString: String,
        onlyRunning: Boolean,
        getConnection: (String?) -> HttpURLConnection?
    ): List<String>? {

        val url = URL(urlString)
        val text = url.readText()

        val lines = text.split("\n").map { it.trim() }
        var urls = lines
            .filter { it.length > 6 }
            .filter { it.substring(0, 4).toUpperCase() == "FILE" }
            .map { it.split("=").last().trim() }
            .filter { URLUtil.isValidUrl(it) && it.substring(0, 4) == "http" }

        if (onlyRunning) {
            urls = urls.filter {
                try {
                    (URL(it).openConnection() as HttpURLConnection)
                        .responseCode == HttpURLConnection.HTTP_OK
                } catch (e: Exception) {
                    false
                }
            }
        }

        return if (urls.isEmpty()) null
        else urls
    }
}