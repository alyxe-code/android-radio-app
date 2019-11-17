package com.p2lem8dev.internetRadio.app.ui.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.service.player.PlayerService
import com.p2lem8dev.internetRadio.app.ui.stations.StationsViewModel
import com.p2lem8dev.internetRadio.app.ui.utils.BindingFragment
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.databinding.FragmentPlayerBinding
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class PlayerFragment : BindingFragment<FragmentPlayerBinding>(R.layout.fragment_player) {

    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var stationsViewModel: StationsViewModel

    private val mActivityCallback = object : PlayerViewModel.ActivityCallback {

        private suspend fun getServiceActionNextPrevious(): String {
            return when {
                SessionRepository.get().isPlaying() -> PlayerService.ACTION_PLAY
                PlayerService.isRunning() -> PlayerService.ACTION_START_WAIT
                else -> PlayerService.ACTION_START
            }
        }

        private fun showSnackBarPlaylistNotLoaded() {
            Snackbar
                .make(binding.root, "Playlist not loaded. Try later", Snackbar.LENGTH_LONG)
                .setAction("Load now") { view ->
                    Toast
                        .makeText(context, "Loading...", Toast.LENGTH_SHORT)
                        .show()
                }
                .show()
        }

        override fun onClickNext(isPlaying: Boolean) {
            if (stationsViewModel.getStations().value != null) {
                val playlist = Playlist.createFrom(
                    stationsViewModel.getStations().value!!,
                    stationsViewModel.selectedStation.value
                )
                stationsViewModel.setSelected(playlist.next)
                GlobalScope.launch {
                    startPlayerService(
                        playlist.next.stationId,
                        getServiceActionNextPrevious()
                    )
                }
            } else {
                showSnackBarPlaylistNotLoaded()
            }
        }

        override fun onClickPrevious(isPlaying: Boolean) {
            if (stationsViewModel.getStations().value != null) {
                val playlist = Playlist.createFrom(
                    stationsViewModel.getStations().value!!,
                    stationsViewModel.selectedStation.value
                )
                stationsViewModel.setSelected(playlist.previous)
                GlobalScope.launch {
                    startPlayerService(
                        playlist.previous.stationId,
                        getServiceActionNextPrevious()
                    )
                }
            } else {
                showSnackBarPlaylistNotLoaded()
            }
        }

        override fun onClickPlayStop(isPlaying: Boolean) {
            GlobalScope.launch {
                startPlayerService(
                    stationsViewModel.selectedStation.value!!.stationId,
                    if (SessionRepository.get().isPlaying()) {
                        PlayerService.ACTION_STOP
                    } else PlayerService.ACTION_PLAY
                )
            }
        }

        override fun onClickChangeFavorite() {
            playerViewModel.stationData.get()?.let {
                GlobalScope.launch {
                    playerViewModel.stationData.set(
                        stationsViewModel.invertFavorite(it)
                    )
                }
            }
        }
    }

    private var jobUpdatePlaying: Job? = null

    private var soundVolume: Int = 0
    private var maxSoundVolume: Int = 15

    private var jobUpdateSoundVolume: Job? = null

    private val soundVolumeHandler = object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            setVolumeByProgress(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            jobUpdateSoundVolume?.cancel()
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            startJobUpdateSoundVolume()
        }
    }

    private fun startJobUpdateSoundVolume() {
        jobUpdateSoundVolume?.cancel()
        jobUpdateSoundVolume = GlobalScope.launch {
            Thread.sleep(100)
            while (true) {
                Thread.sleep(200)
                updateSoundVolumeBySystem()
            }
        }
    }

    private val allStationsLoadedObserver = Observer<List<RadioStation>> {
        onceAllStationsLoaded()
    }

    private fun onceAllStationsLoaded() {
        stationsViewModel.allStations.removeObserver(allStationsLoadedObserver)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupActionBar(null)

        playerViewModel = ViewModelProvider(activity!!).get(PlayerViewModel::class.java)
        playerViewModel.setActivityCallback(mActivityCallback)

        stationsViewModel = ViewModelProvider(activity!!).get(StationsViewModel::class.java)

        stationsViewModel.selectedStation.observe(activity!!, Observer {
            GlobalScope.launch {
                playerViewModel.setStation(it)

                withContext(context = Dispatchers.Main) {
                    setupActionBar(it.title)
                }
            }
        })

        stationsViewModel.allStations.observe(activity!!, allStationsLoadedObserver)

        binding.viewModel = playerViewModel
        binding.executePendingBindings()

        updateSoundVolumeBySystem()

        binding.soundVolume.setOnSeekBarChangeListener(soundVolumeHandler)
        startJobUpdateSoundVolume()
    }

    override fun onResume() {
        super.onResume()
        jobUpdatePlaying = GlobalScope.launch {
            while (true) {
                playerViewModel.isPlaying.set(SessionRepository.get().isPlaying())
                Thread.sleep(100)
            }
        }
    }

    override fun onPause() {
        jobUpdatePlaying?.cancel()
        jobUpdateSoundVolume?.cancel()
        super.onPause()
    }

    fun startPlayerService(stationId: String, action: String = PlayerService.ACTION_PLAY) {
        activity!!.startForegroundService(Intent(context, PlayerService::class.java).apply {
            this.action = action
            putExtra(
                PlayerService.EXTRA_STATION_ID,
                stationId
            )
            putExtra(
                PlayerService.EXTRA_PLAYLIST_SELECTOR,
                if (stationsViewModel.playlistSelector)
                    PlayerService.EXTRA_PLAYLIST_SELECTOR_ANY
                else
                    PlayerService.EXTRA_PLAYLIST_SELECTOR_FAVORITE
            )
        })
    }

    private fun updateSoundVolumeBySystem() {
        activity?.let { activity ->
            (activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager).let {
                maxSoundVolume = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                setVolumeByUnits(it.getStreamVolume(AudioManager.STREAM_MUSIC))
            }
        }
    }

    private fun setVolumeByProgress(progress: Int) {
        soundVolume = (progress.toFloat() / (100.0F / maxSoundVolume)).roundToInt()
        val audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            soundVolume,
            0
        )
    }

    private fun setVolumeByUnits(volume: Int) {
        soundVolume = (volume * 100.0F / maxSoundVolume).roundToInt()
        binding.soundVolume.progress = soundVolume
    }

    private fun setupActionBar(title: String?) {
        activity?.let { activity ->
            (activity as AppCompatActivity).let { appCompatActivity ->
                appCompatActivity.supportActionBar?.let {
                    it.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
                    it.setCustomView(R.layout.actionbar_player_layout)
                    it.customView.findViewById<TextView>(R.id.actionbar_title)
                        .text = title ?: getString(R.string.title_player)
                    it.customView.findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
                        openSettings()
                    }
                }
            }
        }
    }

    private fun openSettings() {
        Toast.makeText(context!!, "Open Settings", Toast.LENGTH_LONG).show()
    }
}

