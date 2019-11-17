package com.p2lem8dev.internetRadio.app.service.sync

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.IBinder
import android.util.Log
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.MainActivity
import com.p2lem8dev.internetRadio.app.utils.notification.NotificationFactory
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SyncService : Service() {

    private var _isSynchronized: Boolean = false
    /**
     * True after synchronization process finally finished
     */
    public val isSynchronized: Boolean
        get() = _isSynchronized

    private var _isRunning: Boolean = false
    /**
     * True when synchronizing
     */
    public val isRunning: Boolean
        get() = _isRunning

    private var imagesDownloadDir: String? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        imagesDownloadDir = intent.getStringExtra(EXTRA_IMAGES_DOWNLOAD_DIR)

        when (intent.action) {
            ACTION_START_LOAD_ALL -> handleActionStartLoadAll()
            ACTION_START_LOAD_BASE -> handleActionStartLoadBase()
            ACTION_STOP -> handleActionStop()
        }
        return START_STICKY
    }

    private fun handleActionStartLoadAll() {
        instance = this
    }

    private var syncStationsAmount = 0

    private fun handleActionStartLoadBase() {
        Log.d("SYNC_SERVICE", "Loading base...")
        createOrUpdateNotification()
        GlobalScope.launch {
            RadioStationRepository.get().loadRadiosFromAllPages(
                onlyRunning = true,
                saveImagesDirectory = imagesDownloadDir!!
            ) {
                createOrUpdateNotification(it.title)
                onNextLoaded?.invoke()
                syncStationsAmount++
                checkStopNotification()
            }
        }
        instance = this
    }

    private fun handleActionStop() {
        stopSelf()
    }

    private fun createOrUpdateNotification(title: String? = null) {
        val notification = NotificationFactory(applicationContext)
            .createInfiniteLoadingType("Synchronization", title ?: "")
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(applicationContext, R.drawable.ic_close_black_24dp),
                    "Cancel",
                    PendingIntent.getService(
                        applicationContext,
                        0,
                        Intent(applicationContext, SyncService::class.java).apply {
                            action = ACTION_STOP
                        },
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                )
            )
            .bindToActivity(MainActivity::class.java)
            .build()

        NotificationFactory.registerNotificationChannel(
            applicationContext,
            NotificationFactory.NotificationChannelType.Synchronization
        )

        startForeground(NotificationFactory.NOTIFICATION_SYNC_ID, notification)
    }

    private var jobCheckStopNotification: Job? = null

    private fun checkStopNotification() {
        // stop job
        jobCheckStopNotification?.cancel()
        // start again
        jobCheckStopNotification = GlobalScope.launch {
            Thread.sleep(TIME_STOP_NOTIFICATION_TIMEOUT)

            // stop after timeout | it won't be restarted
            // create notification to show that process has been finished
            val notification = NotificationFactory(applicationContext).createTextStyle(
                "Synchronization",
                "Synchronization has been finished. Synced $syncStationsAmount stations"
            ).build()

            stopForeground(true)
            startForeground(NotificationFactory.NOTIFICATION_DEFAULT_ID, notification)
            stopForeground(false)

            // cancel job
            jobCheckStopNotification?.cancel()
        }
    }

    companion object {
        private const val ACTION_START_LOAD_ALL = "action::start::load_all"
        private const val ACTION_START_LOAD_BASE = "action::start::load_base"
        private const val ACTION_STOP = "action:stop"

        private const val EXTRA_IMAGES_DOWNLOAD_DIR = "extra::images_download_dir"

        private const val TIME_STOP_NOTIFICATION_TIMEOUT = 10000L // 10 sec

        private var instance: SyncService? = null
        fun getInstance(): SyncService? {
            return instance
        }

        private var onNextLoaded: (() -> Unit)? = null

        fun start(
            context: Context,
            imagesDownloadDir: String,
            loadAll: Boolean = false,
            onNextLoaded: (() -> Unit)? = null
        ) {
            if (instance != null && instance!!.isRunning) return
            context.startForegroundService(Intent(context, SyncService::class.java).apply {
                action = if (loadAll) ACTION_START_LOAD_ALL else ACTION_START_LOAD_BASE
                putExtra(EXTRA_IMAGES_DOWNLOAD_DIR, imagesDownloadDir)
            })
            this.onNextLoaded = onNextLoaded
        }

        fun stop(context: Context) {
            context.startForegroundService(Intent(context, SyncService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }
}
