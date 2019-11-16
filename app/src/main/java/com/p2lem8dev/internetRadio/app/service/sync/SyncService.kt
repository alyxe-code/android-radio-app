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
import com.p2lem8dev.internetRadio.app.InternetRadioApp
import com.p2lem8dev.internetRadio.app.utils.notification.NotificationFactory
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import com.p2lem8dev.internetRadio.sync.SyncActivity
import kotlinx.coroutines.GlobalScope
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

    private var savedStationsAmount: Int = 0
    private var loadedStationsAmount: Int = 0

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        when (intent.action) {
            ACTION_START -> handleActionStart()
            ACTION_STOP -> handleActionStop()
        }
        return START_STICKY
    }

    private fun handleActionStart() {
        GlobalScope.launch {
            RadioStationRepository.get()
            createOrUpdateNotification(loadedStationsAmount, savedStationsAmount)
            RadioStationRepository.get().sync(
                (application as InternetRadioApp).getImagesSaveDirectory(),
                onLoad = {
                    loadedStationsAmount++
                    createOrUpdateNotification(loadedStationsAmount, savedStationsAmount)
                },
                onSave = {
                    Log.d("SYNC_BG", "Saving station#${it.id} ${it.title}")
                    savedStationsAmount++
                }
            )
        }
        instance = this
    }

    private fun handleActionStop() {
        stopForeground(false)
        stopSelf()
    }

    private var isForeground = false

    private fun createOrUpdateNotification(allAmount: Int, validAmount: Int) {
        val notification = NotificationFactory(applicationContext)
            .createTextStyle("Syncing", "ALL $allAmount | VALID $validAmount")
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
            .bindToActivity(SyncActivity::class.java)
            .build()

        if (isForeground) {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NotificationFactory.NOTIFICATION_SYNC_ID, notification)
        } else {
            NotificationFactory.registerNotificationChannel(
                applicationContext,
                NotificationFactory.NotificationChannelType.Synchronization
            )
            startForeground(NotificationFactory.NOTIFICATION_SYNC_ID, notification)
            isForeground = true
        }
    }

    companion object {
        const val ACTION_START = "action::start"
        const val ACTION_STOP = "action:stop"

        private var instance: SyncService? = null
        fun getInstance(): SyncService? {
            return instance
        }

        fun start(context: Context) {
            if (instance != null) return
            context.startForegroundService(Intent(context, SyncService::class.java).apply {
                action = ACTION_START
            })
        }

        fun stop(context: Context) {
            context.startForegroundService(Intent(context, SyncService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }
}
