package com.p2lem8dev.internetRadio.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.service.player.PlayerService
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation

class NotificationCreator(private val context: Context) {

    private var notificationBuilder: Notification.Builder? = null
    private val playerServiceIntent = Intent(context, PlayerService::class.java)

    fun addActionClose(): NotificationCreator {
        notificationBuilder?.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_close_black_24dp),
                "CLOSE",
                PendingIntent.getService(
                    context,
                    0,
                    playerServiceIntent.apply {
                        action =
                            PlayerService.ACTION_KILL
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            ).build()
        )

        return this
    }

    fun addActionPlay(stationId: String, playlistAny: Boolean): NotificationCreator {
        notificationBuilder?.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_player_play_normal),
                "PLAY_STOP",
                PendingIntent.getService(
                    context,
                    0,
                    Intent(context, PlayerService::class.java).apply {
                        action = PlayerService.ACTION_PLAY
                        putExtra(PlayerService.EXTRA_STATION_ID, stationId)
                        putExtra(PlayerService.EXTRA_PLAYLIST_SELECTOR, playlistAny)
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            ).build()
        )

        return this
    }

    fun addActionPlayNext(stationId: String, playlistAny: Boolean): NotificationCreator {
        notificationBuilder?.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_player_next_normal),
                "Next",
                PendingIntent.getService(
                    context,
                    0,
                    Intent(context, PlayerService::class.java).apply {
                        action = PlayerService.ACTION_PLAY_NEXT
                        putExtra(PlayerService.EXTRA_STATION_ID, stationId)
                        putExtra(PlayerService.EXTRA_PLAYLIST_SELECTOR, playlistAny)
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            ).build()
        )

        return this
    }

    fun addActionPlayPrevious(stationId: String, playlistAny: Boolean): NotificationCreator {
        notificationBuilder?.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(
                    context,
                    R.drawable.ic_player_previous_normal
                ),
                "Previous",
                PendingIntent.getService(
                    context,
                    0,
                    playerServiceIntent.apply {
                        action = PlayerService.ACTION_PLAY_PREVIOUS
                        putExtra(PlayerService.EXTRA_STATION_ID, stationId)
                        putExtra(PlayerService.EXTRA_PLAYLIST_SELECTOR, playlistAny)
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            ).build()
        )

        return this
    }

    fun addActionStop(stationId: String, playlistAny: Boolean): NotificationCreator {
        notificationBuilder?.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_player_stop_normal),
                "PLAY_STOP",
                PendingIntent.getService(
                    context,
                    0,
                    playerServiceIntent.apply {
                        action = PlayerService.ACTION_STOP
                        putExtra(PlayerService.EXTRA_STATION_ID, stationId)
                        putExtra(PlayerService.EXTRA_PLAYLIST_SELECTOR, playlistAny)
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            ).build()
        )

        return this
    }

    fun addActionChangeFavorite(
        stationId: String,
        playlistAny: Boolean,
        isFavorite: Boolean
    ): NotificationCreator {
        notificationBuilder?.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(
                    context,
                    if (isFavorite) R.drawable.ic_favorite_white_24dp
                    else R.drawable.ic_favorite_border_black_24dp
                ),
                "LIKE",
                PendingIntent.getService(
                    context,
                    0,
                    playerServiceIntent.apply {
                        action = PlayerService.ACTION_CHANGE_FAVORITE
                        putExtra(PlayerService.EXTRA_STATION_ID, stationId)
                        putExtra(
                            PlayerService.EXTRA_PLAYLIST_SELECTOR,
                            if (playlistAny) PlayerService.EXTRA_PLAYLIST_SELECTOR_ANY
                            else PlayerService.EXTRA_PLAYLIST_SELECTOR_FAVORITE
                        )
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            ).build()
        )

        return this
    }

    fun createMediaStyle(
        station: RadioStation,
        sessionToken: MediaSession.Token
    ): NotificationCreator {
        notificationBuilder = Notification.Builder(
            context,
            PlayerService.NOTIFICATION_CHANNEL_DEFAULT
        )
            .setStyle(
                Notification.MediaStyle()
                    .setShowActionsInCompactView(1, 2, 3)
                    .setMediaSession(sessionToken)
            )
            .setSmallIcon(R.drawable.ic_player_play_normal)
            .setLargeIcon(BitmapFactory.decodeFile(station.imageUrl))
            .setContentTitle(station.title)
            .setContentText(station.stationId)

        return this
    }

    fun build(): Notification? {
        return notificationBuilder?.build()
    }

}