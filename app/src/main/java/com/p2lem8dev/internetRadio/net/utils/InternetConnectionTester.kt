package com.p2lem8dev.internetRadio.net.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class InternetConnectionTester {

    private var _isConnectedAndActive: Boolean = false
    var isConnectedAndActive
        get() = _isConnectedAndActive
        private set(value) {
            _isConnectedAndActive = value
        }

    private var jobTest: Job? = null

    private var hostname: String? = null

    fun start() {
        while (true) {
            try {
                hostname = InetAddress.getByName(TEST_HOSTNAME).hostAddress
                break
            } catch (e: Exception) {
                isConnectedAndActive = false
            }
        }
        Log.d("SYNC", "It will connect to $hostname")

        jobTest = GlobalScope.launch(context = Dispatchers.IO) {
            while (true) {
                isConnectedAndActive = tryConnectToServer()
                Thread.sleep(TEST_TIME_INTERVAL)
            }
        }
        jobTest?.start()
    }

    fun stop() {
        jobTest?.cancel()
    }

    private fun tryConnectToServer() = try {
        val sock = Socket()
        sock.connect(InetSocketAddress(hostname, TEST_PORT))
        sock.close()
        true
    } catch (e: Exception) {
        Log.w(
            "SYNC_NET", "ERROR Connection | ${e.message}" +
                    e.stackTrace.joinToString("\n")
        )
        false
    }


    companion object {
        const val TEST_HOSTNAME = "www.radio-tochka.com"
        const val TEST_PORT = 80
        const val TEST_TIME_INTERVAL = 1000L

        private var instance: InternetConnectionTester? = null

        private fun getInstance(): InternetConnectionTester {
            if (instance == null) {
                instance = InternetConnectionTester()
                instance!!.start()
            }
            return instance!!
        }

        fun waitConnection() {
            if (getInstance().isConnectedAndActive) return
            var connection: Boolean
            while (true) {
                connection = !getInstance().isConnectedAndActive
                if (connection) break
                Thread.sleep(TEST_TIME_INTERVAL / 10)
            }
            getInstance().stop()
        }
    }
}