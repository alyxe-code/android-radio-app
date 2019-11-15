package com.p2lem8dev.internetRadio.app.service.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.p2lem8dev.internetRadio.app.InternetRadioApp
import com.p2lem8dev.internetRadio.app.service.NotificationFactory
import com.p2lem8dev.internetRadio.net.api.BaseRadioInfo
import com.p2lem8dev.internetRadio.net.api.RadioTochkaAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SyncService : Service() {

    /**
     * Keeps synchronization state value until service stops
     */
    private var syncStateValue: SyncState = SyncState.NotStarted
    private var prevSyncStateValue: SyncState = SyncState.NotStarted

    enum class SyncState {
        /**
         * Used before starting
         */
        NotStarted,
        /**
         * Used when service is configured and ready to be started
         */
        Configured,
        /**
         * Loading pages process has been started
         */
        LoadingPages,
        /**
         * Links validation has been started
         */
        Validation,
        /**
         * Links configuration has been started
         */
        ConfiguringLinks,
        /**
         * Fill valid stations. Invalid are already removed from queue
         */
        Filling,
        /**
         * Loading images for valid stations
         */
        LoadingImage,
        /**
         * Saving valid stations to database and remove old invalid values
         */
        Saving,
        /**
         * Synchronization is finished
         */
        Finished,
        /**
         * Something gone wrong
         */
        Error
    }

    private val api: RadioTochkaAPI = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(InternetRadioApp.RADIO_TOCHKA_API_HOST)
        .build()
        .create(RadioTochkaAPI::class.java)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(
            NotificationFactory.NOTIFICATION_DEFAULT_ID,
            NotificationFactory(applicationContext)
                .createTextStyle("Sync", "Sync")
                .build()
        )

        return START_STICKY
    }

    fun handelActionStart() {
        GlobalScope.launch(context = Dispatchers.IO) {
            syncStateValue = SyncState.LoadingPages
            val stationsList = loadStationsList().filter { filterStations(it) }
        }
    }

    /**
     * Load all available stations
     */
    private suspend fun loadStationsList(): List<BaseRadioInfo> {
        val count = api.getCountRadios().items.toInt()
        val stations = arrayListOf<BaseRadioInfo>()

        var pageIndex = 0
        while (stations.size < count) {
            val pageItems = api.getPage(pageIndex)
            if (pageItems.isEmpty()) break
            stations.addAll(pageItems)
            pageIndex++
        }

        return stations
    }

    private fun filterStations(station: BaseRadioInfo): Boolean {
        syncStateValue = SyncState.Validation

        return true
    }


    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, SyncService::class.java))
        }
    }
}
