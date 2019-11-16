package com.p2lem8dev.internetRadio.app.utils.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.sync.SyncActivity

open class NotificationFactory(protected val context: Context) {

    protected var notificationBuilder: Notification.Builder? = null
    protected lateinit var intent: Intent

    protected val applicationIcon: Icon =
        Icon.createWithResource(context, R.drawable.ic_player_stop_normal)

    fun addAction(action: Notification.Action.Builder): NotificationFactory {
        notificationBuilder?.addAction(action.build())
        return this
    }

    fun createTextStyle(title: String, text: String): NotificationFactory {
        notificationBuilder = Notification.Builder(context, NOTIFICATION_DEFAULT_CHANNEL_ID)
            .setSmallIcon(applicationIcon)
            .setContentTitle(title)
            .setContentText(text)

        return this
    }

    fun createInfiniteLoadingType(title: String, text: String?): NotificationFactory {
        notificationBuilder = Notification.Builder(context, NOTIFICATION_SYNC_CHANNEL_ID)
            .setSmallIcon(applicationIcon)
            .setContentTitle(title)
            .setProgress(0, 0, true)

        if (text != null) {
            notificationBuilder?.setContentText(text)
        }

        return this
    }

    fun createPlayerWidgetNotification(station: RadioStation, sessionToken: MediaSession.Token) =
        PlayerWidgetNotificationFactory.createNotification(context, station, sessionToken)

    fun bindToActivity(activity: Class<out Activity>): NotificationFactory {
        notificationBuilder?.setContentIntent(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, activity),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        return this
    }

    fun build(): Notification? {
        return notificationBuilder?.build()
    }


    enum class NotificationChannelType {
        Default, PlayerWidget, Synchronization
    }

    companion object {

        const val NOTIFICATION_DEFAULT_CHANNEL_ID = "notification::default"
        const val NOTIFICATION_DEFAULT_CHANNEL_NAME = "Default"
        const val NOTIFICATION_DEFAULT_ID = 1

        const val NOTIFICATION_PLAYER_CHANNEL_ID = "notification::player"
        const val NOTIFICATION_PLAYER_CHANNEL_NAME = "Player widget"
        const val NOTIFICATION_PLAYER_ID = 2

        const val NOTIFICATION_SYNC_CHANNEL_ID = "notification::sync"
        const val NOTIFICATION_SYNC_CHANNEL_NAME = "Synchronization"
        const val NOTIFICATION_SYNC_ID = 3

        private fun createNotificationChannel(type: NotificationChannelType): NotificationChannel {
            val id: String
            val name: String
            val importance: Int
            when (type) {
                NotificationChannelType.Default -> {
                    id = NOTIFICATION_DEFAULT_CHANNEL_ID
                    name = NOTIFICATION_DEFAULT_CHANNEL_NAME
                    importance = NotificationManager.IMPORTANCE_DEFAULT
                }
                NotificationChannelType.PlayerWidget -> {
                    id = NOTIFICATION_PLAYER_CHANNEL_ID
                    name = NOTIFICATION_PLAYER_CHANNEL_NAME
                    importance = NotificationManager.IMPORTANCE_LOW
                }
                NotificationChannelType.Synchronization -> {
                    id = NOTIFICATION_SYNC_CHANNEL_ID
                    name = NOTIFICATION_SYNC_CHANNEL_NAME
                    importance = NotificationManager.IMPORTANCE_LOW
                }
            }
            return NotificationChannel(id, name, importance)
        }

        public fun registerNotificationChannel(
            context: Context,
            notificationChannel: NotificationChannel
        ) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        public fun registerNotificationChannel(context: Context, type: NotificationChannelType) =
            registerNotificationChannel(context, createNotificationChannel(type))

    }
}