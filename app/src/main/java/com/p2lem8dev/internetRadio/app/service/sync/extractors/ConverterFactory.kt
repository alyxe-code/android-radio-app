package com.p2lem8dev.internetRadio.app.service.sync.extractors

import android.util.Log
import android.view.inputmethod.InputContentInfo
import com.p2lem8dev.internetRadio.net.utils.InternetConnectionTester
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

interface ConverterFactory {
    fun getSupportedContentTypes(): List<String>? {
        return null
    }

    fun getSupportedFileExtensions(): List<String>? {
        return null
    }

    fun extract(
        urlString: String,
        onlyRunning: Boolean,
        getConnection: (String?) -> HttpURLConnection?
    ): List<String>? {
        return null
    }

    fun readURLContent(url: URL): String? {
        var result: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(url.openStream()))
            var inputLine: String?
            val stringBuilder = StringBuilder()

            val start = System.currentTimeMillis()
            while (true) {
                try {
                    inputLine = reader.readLine()
                    if (inputLine == null || inputLine == "null") break
                    stringBuilder.append(inputLine)
                } catch (e: Exception) {
                    InternetConnectionTester.waitConnection()
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime - start > DefaultConverterFactory.MAX_READ_TIME) break
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

    companion object {
        fun getDefaultExtractor() = DefaultConverterFactory()

        val URL_REGEX = Regex("(http|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?")
    }
}