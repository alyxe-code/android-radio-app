package com.p2lem8dev.internetRadio.app.service.sync.extractors

import java.net.HttpURLConnection

interface ConverterFactory {
    fun getSupportedContentTypes(): List<String>? { return null }
    fun getSupportedFileExtensions(): List<String>? { return null }
    fun extract(urlString: String, onlyRunning: Boolean, getConnection: (String?) -> HttpURLConnection?): List<String>? { return null }

    companion object {
        fun getDefaultExtractor() = DefaultConverterFactory()
    }
}