package com.p2lem8dev.internetRadio.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.utils.notification.PlayerWidgetNotificationFactory
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation

open class NotificationFactory(protected val context: Context) {

    protected var notificationBuilder: Notification.Builder? = null
    protected lateinit var intent: Intent

    protected val applicationIcon: Icon =
        Icon.createWithResource(context, R.drawable.ic_player_stop_normal)


    fun createTextStyle(title: String, text: String): NotificationFactory {
        notificationBuilder = Notification.Builder(context, NOTIFICATION_DEFAULT_CHANNEL_ID)
            .setSmallIcon(applicationIcon)
            .setContentTitle(title)
            .setContentText(text)

        return this
    }

    fun createPlayerWidgetNotification(station: RadioStation, sessionToken: MediaSession.Token) =
        PlayerWidgetNotificationFactory.createNotification(context, station, sessionToken)

    fun build(): Notification? {
        return notificationBuilder?.build()
    }


    enum class NotificationChannelType {
        Default, PlayerWidget
    }

    companion object {
        private const val NOTIFICATION_DEFAULT_CHANNEL_ID = "notification::default"
        private const val NOTIFICATION_DEFAULT_CHANNEL_NAME = "Default"
        const val NOTIFICATION_DEFAULT_ID = 1

        private const val NOTIFICATION_PLAYER_CHANNEL_ID = "notification::player"
        private const val NOTIFICATION_PLAYER_CHANNEL_NAME = "Player widget"
        const val NOTIFICATION_PLAYER_ID = 2

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