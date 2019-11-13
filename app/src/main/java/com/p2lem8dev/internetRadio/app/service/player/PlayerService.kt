package com.p2lem8dev.internetRadio.app.service.player

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import com.p2lem8dev.internetRadio.app.MainActivity
import com.p2lem8dev.internetRadio.app.service.NotificationCreator
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.RadioDatabase
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class PlayerService : Service() {

    private var stationData: RadioStation? = null
    private var stationId: String? = null
    private var playlistSelector: Boolean = true

    private lateinit var mediaSession: MediaSession
    private lateinit var mediaController: MediaController
    private var mExoPlayer: SimpleExoPlayer? = null

    private val mPlaybackStateBuilder: PlaybackState.Builder = PlaybackState.Builder()
        .setActions(
            PlaybackState.ACTION_PLAY
                    or PlaybackState.ACTION_PAUSE
                    or PlaybackState.ACTION_STOP
        )

    private val mMediaSessionCallback = object : MediaSession.Callback() {
        override fun onPlay() {
            super.onPlay()
            Log.d("PLAYER_SERVICE_MEDIA", "Handle onPlay")
            requireNotNull(stationData) { IllegalStateException("Station Data must not be null") }
            GlobalScope.launch {
                play()
                setMediaSessionPlaying()
            }
        }

        override fun onPause() {
            super.onPause()
            Log.d("PLAYER_SERVICE_MEDIA", "Handle onPause")
            setMediaSessionStopped()
        }

        override fun onStop() {
            super.onStop()
            Log.d("PLAYER_SERVICE_MEDIA", "Handle onStop")
            stop()
            setMediaSessionStopped()
        }
    }

    private var isPlaying = false

    private val commandsRequireStation = listOf(
        ACTION_START,
        ACTION_START_WAIT,
        ACTION_PLAY,
        ACTION_PLAY_PREVIOUS,
        ACTION_PLAY_NEXT,
        ACTION_CHANGE_FAVORITE
    )
    private val commandsRequireSticky = listOf(
        ACTION_START_WAIT,
        ACTION_PLAY,
        ACTION_PLAY_PREVIOUS,
        ACTION_PLAY_NEXT,
        ACTION_STOP,
        ACTION_CHANGE_FAVORITE
    )

    private lateinit var playlist: Playlist


    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService {
            return this@PlayerService
        }
    }


    override fun onCreate() {
        super.onCreate()
        playlist = Playlist()
        playerService = this

        mediaSession = MediaSession(applicationContext, "PLAYER_MEDIA").apply {
            setPlaybackState(mPlaybackStateBuilder.build())
            setCallback(mMediaSessionCallback)

            val activityIntent = Intent(applicationContext, MainActivity::class.java)
            setSessionActivity(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    activityIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
            )
        }

        mediaController = MediaController(applicationContext, mediaSession.sessionToken)
    }

    override fun onDestroy() {
        super.onDestroy()
        playerService = null
        mediaSession.release()
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        GlobalScope.launch {
            if (commandsRequireStation.contains(intent.action)) {
                // validate stationId
                var stationId = intent.getStringExtra(EXTRA_STATION_ID)
                require(!(stationId == null && this@PlayerService.stationId == null)) {
                    IllegalArgumentException(
                        "StationId cannot be null"
                    )
                }
                if (this@PlayerService.stationId != null && stationId == null) {
                    stationId = this@PlayerService.stationId
                }

                // update stationId
                this@PlayerService.stationId = stationId
                stationData = RadioRepository.get().findByStationId(this@PlayerService.stationId!!)
                checkNotNull(stationData) { IllegalArgumentException("Station#${this@PlayerService.stationId} not found in database") }

                // update playlist selector
                val playlistSelector = intent.getBooleanExtra(EXTRA_PLAYLIST_SELECTOR, true)
                if (playlistSelector != this@PlayerService.playlistSelector || playlist.isEmpty()) {
                    this@PlayerService.playlistSelector = playlistSelector
                    playlist = Playlist()
                    playlist.loadAsync(playlistSelector)
                }

                // ensure everything is fine
                check(!playlist.isEmpty()) { IllegalStateException("Playlist is empty") }

                // update playlist data
                playlist.setCurrentByStationId(this@PlayerService.stationId!!)
            }

            // handle commands
            when (intent.action) {
                ACTION_START -> handleActionStart()
                ACTION_START_WAIT -> handleActionStartWait()
                ACTION_PLAY -> handleActionPlay()
                ACTION_PLAY_NEXT -> handleActionPlayNext()
                ACTION_PLAY_PREVIOUS -> handleActionPlayPrevious()
                ACTION_STOP -> handleActionStop()
                ACTION_CHANGE_FAVORITE -> handleActionChangeFavorite()
                ACTION_KILL -> handleActionKill()
            }
        }

        return if (commandsRequireSticky.contains(intent.action)) START_STICKY
        else START_NOT_STICKY
    }

    private fun handleActionStart() = GlobalScope.launch {
        createOrUpdateNotification()
        SessionRepository.get().updateLastRunningStation(stationId!!)
        Thread.sleep(10)
        stopSelf()
    }

    private fun handleActionStartWait() = GlobalScope.launch {
        createOrUpdateNotification()
        SessionRepository.get().updateLastRunningStation(stationId!!)
        Thread.sleep(10)
    }

    private fun handleActionPlay() = GlobalScope.launch {
        play()
        createOrUpdateNotification()
    }

    private fun handleActionPlayNext() = GlobalScope.launch {
        play()
        createOrUpdateNotification()
    }

    private fun handleActionPlayPrevious() = GlobalScope.launch {
        play()
        createOrUpdateNotification()
    }

    private fun handleActionStop() {
        stop()
        createOrUpdateNotification()
    }

    private fun handleActionKill() {
        stop()
        mExoPlayer?.release()
        stopSelf()
    }

    private fun handleActionChangeFavorite() = GlobalScope.launch {
        stationData?.let { station ->
            station.isFavorite = !station.isFavorite
            stationData = station
            RadioRepository.get().setFavorite(station.stationId, station.isFavorite)
            createOrUpdateNotification()
        }
    }


    private fun createOrUpdateNotification() {
        createOrUpdateNotificationChannel()

        // Ensure playlist not empty
        check(!playlist.isEmpty()) { IllegalStateException("Playlist is empty") }

        // Create notification builder
        val notification = NotificationCreator(applicationContext)
            .createMediaStyle(stationData!!, mediaSession.sessionToken)

        // Close
        notification.addActionClose()
        // Play previous
        notification.addActionPlayPrevious(playlist.previous.stationId, playlistSelector)
        // Play or Stop
        if (isPlaying) notification.addActionStop(playlist.current.stationId, playlistSelector)
        else notification.addActionPlay(playlist.current.stationId, playlistSelector)
        // Play next
        notification.addActionPlayNext(playlist.next.stationId, playlistSelector)
        // Change favorite
        notification.addActionChangeFavorite(
            stationId!!,
            playlistSelector,
            stationData!!.isFavorite
        )

        // show notification
        startForeground(1, notification.build())
    }

    private fun createOrUpdateNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_DEFAULT,
                "Player",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }


    private fun prepareExoPlayer(url: String) {
        val userAgent = Util.getUserAgent(applicationContext, applicationContext.packageName)
        Log.d("PLAYER_SERVICE", "userAgent $userAgent")
        Log.d("PLAYER_SERVICE", "packageName ${applicationContext.packageName}")

        val mediaSource = ExtractorMediaSource.Factory(
            DefaultDataSourceFactory(applicationContext, userAgent)
        )
            .setExtractorsFactory(DefaultExtractorsFactory())
            .createMediaSource(Uri.parse(url))

        mExoPlayer?.let {
            it.stop(true)
            it.release()
            mExoPlayer = null
        }

        mExoPlayer = ExoPlayerFactory.newSimpleInstance(
            DefaultRenderersFactory(applicationContext),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )?.also {
            it.audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()
            it.prepare(mediaSource)
            it.playWhenReady = true
        }
    }

    private fun setMediaSessionPlaying() {
        mediaSession.isActive = true
        mediaSession.setPlaybackState(
            mPlaybackStateBuilder.setState(
                PlaybackState.STATE_PLAYING,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1F
            ).build()
        )
    }

    private fun setMediaSessionStopped() {
        mediaSession.isActive = false
        mediaSession.setPlaybackState(
            mPlaybackStateBuilder.setState(
                PlaybackState.STATE_STOPPED,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1F
            ).build()
        )
    }

    private suspend fun play() {
        isPlaying = true
        stationData?.let {
            prepareExoPlayer(it.links[0])
            SessionRepository.get().setRadioRunning(stationId!!)
        }
    }

    private fun stop() {
        isPlaying = false
        mExoPlayer?.stop(true)
        GlobalScope.launch {
            SessionRepository.get().setRadioStopped(stationId!!)
        }
    }

    companion object {
        // commands
        const val ACTION_START = "action::start"
        const val ACTION_START_WAIT = "action::start_wait"
        const val ACTION_PLAY = "action::play"
        const val ACTION_PLAY_NEXT = "action::next"
        const val ACTION_PLAY_PREVIOUS = "action::previous"
        const val ACTION_STOP = "action:stop"
        const val ACTION_KILL = "action::kill"
        const val ACTION_CHANGE_FAVORITE = "action::change_favorite"

        // station id
        const val EXTRA_STATION_ID = "extra:station_id"
        // playlist selector
        const val EXTRA_PLAYLIST_SELECTOR = "extra::playlist_selector"
        const val EXTRA_PLAYLIST_SELECTOR_ANY = Playlist.PLAYLIST_SELECTOR_ANY
        const val EXTRA_PLAYLIST_SELECTOR_FAVORITE = Playlist.PLAYLIST_SELECTOR_FAVORITE

        const val NOTIFICATION_CHANNEL_DEFAULT = "notification::default"

        private var playerService: PlayerService? = null

        fun isRunning(): Boolean {
            return playerService != null
        }
    }
}