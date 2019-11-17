package com.p2lem8dev.internetRadio.app.service.player

import android.app.*
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
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.p2lem8dev.internetRadio.app.MainActivity
import com.p2lem8dev.internetRadio.app.utils.notification.NotificationFactory
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
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
                var stationId =
                    intent.getStringExtra(EXTRA_STATION_ID) ?: this@PlayerService.stationId
                require(stationId != null) { IllegalStateException("Station Id is required") }

                // update stationId
                this@PlayerService.stationId = stationId
                stationData = RadioStationRepository.get()
                    .findStation(this@PlayerService.stationId!!)

                checkNotNull(stationData) {
                    // TODO: Handle it
                    IllegalArgumentException(
                        "Station#${this@PlayerService.stationId} not found in database"
                    )

                }

                // update playlist selector
                val playlistSelector = intent.getBooleanExtra(EXTRA_PLAYLIST_SELECTOR, true)
                this@PlayerService.playlistSelector = playlistSelector
            }

            // rebuild playlist each time because data could be changed
            // TODO: Add checker for this
            playlist = Playlist()
            playlist.loadAsync(playlistSelector) {
                // update playlist data
                playlist.setCurrentByStationId(this@PlayerService.stationId!!)

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

    private fun handleActionPlayNext() = handleActionPlay()

    private fun handleActionPlayPrevious() = handleActionPlay()

    private fun handleActionStop() {
        stop()
        createOrUpdateNotification(true)
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
            RadioStationRepository.get().setFavorite(station.stationId, station.isFavorite)
            createOrUpdateNotification()
        }
    }

    private fun createOrUpdateNotification(allowCloseNotification: Boolean = false) {
        requireNotNull(!playlist.isEmpty()) { "Playlist is empty!!!!" }
        requireNotNull(stationId) { "Station Id is required but null" }
        requireNotNull(stationData) { "Station data required but null" }

        createOrUpdateNotificationChannel()

        // Create notification builder
        val notification = NotificationFactory(applicationContext)
            .createPlayerWidgetNotification(stationData!!, mediaSession.sessionToken)

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
        notification.bindToActivity(MainActivity::class.java)

        // Show notification
        startForeground(NotificationFactory.NOTIFICATION_PLAYER_ID, notification.build())
        if (allowCloseNotification) {
            stopForeground(false)
        }
    }

    private fun createOrUpdateNotificationChannel() {
        NotificationFactory.registerNotificationChannel(
            applicationContext,
            NotificationFactory.NotificationChannelType.PlayerWidget
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

        // leave issue when several players are playing
        mExoPlayer?.let {
            try {
                it.stop(true)
            } catch (e: Exception) {
            }
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
            it.addListener(object : Player.EventListener {
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                }

                override fun onSeekProcessed() {
                }

                override fun onTracksChanged(
                    trackGroups: TrackGroupArray?,
                    trackSelections: TrackSelectionArray?
                ) {
                }

                override fun onPlayerError(error: ExoPlaybackException?) {
                    GlobalScope.launch {
                        requireNotNull(stationId)
                        playlist.removeByStationId(stationId!!)
                        RadioStationRepository.get().deleteByStationId(stationId!!)
                        stationData = playlist.current
                        stationId = stationData!!.stationId
                        handleActionPlay()
                        withContext(context = Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Unable to play radio", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onLoadingChanged(isLoading: Boolean) {
                }

                override fun onPositionDiscontinuity(reason: Int) {
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                }

                override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                }

            })
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
        setMediaSessionPlaying()
    }

    private fun stop() {
        isPlaying = false
        if (mExoPlayer == null) {
            Log.wtf("PLAYER", "ExoPlayer is null")
        }
        mExoPlayer?.stop(true)
        GlobalScope.launch {
            SessionRepository.get().setRadioStopped(stationId!!)
        }
        setMediaSessionStopped()
    }

    companion object {
        // commands
        const val ACTION_START = "action::start"
        const val ACTION_START_WAIT = "action::start_wait"
        const val ACTION_PLAY = "action::play"
        const val ACTION_PLAY_NEXT = "action::next"
        const val ACTION_PLAY_PREVIOUS = "action::previous"
        const val ACTION_STOP = "action::stop"
        const val ACTION_KILL = "action::kill"
        const val ACTION_CHANGE_FAVORITE = "action::change_favorite"

        // station id
        const val EXTRA_STATION_ID = "extra::station_id"
        // playlist selector
        const val EXTRA_PLAYLIST_SELECTOR = "extra::playlist_selector"
        const val EXTRA_PLAYLIST_SELECTOR_ANY = Playlist.PLAYLIST_SELECTOR_ALL
        const val EXTRA_PLAYLIST_SELECTOR_FAVORITE = Playlist.PLAYLIST_SELECTOR_FAVORITE

        const val NOTIFICATION_CHANNEL_DEFAULT = "notification::default"

        private var playerService: PlayerService? = null

        fun isRunning(): Boolean {
            return playerService != null
        }
    }
}