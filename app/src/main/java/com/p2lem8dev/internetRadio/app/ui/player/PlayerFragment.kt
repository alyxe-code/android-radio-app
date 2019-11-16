package com.p2lem8dev.internetRadio.app.ui.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.service.player.PlayerService
import com.p2lem8dev.internetRadio.app.ui.stations.StationsViewModel
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.databinding.FragmentPlayerBinding
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PlayerFragment : Fragment() {

    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var binding: FragmentPlayerBinding
    private lateinit var stationsViewModel: StationsViewModel

    private val mActivityCallback = object : PlayerViewModel.ActivityCallback {

        private suspend fun getServiceActionNextPrevious(): String {
            return when {
                SessionRepository.get().isPlaying() -> PlayerService.ACTION_PLAY
                PlayerService.isRunning() -> PlayerService.ACTION_START_WAIT
                else -> PlayerService.ACTION_START
            }
        }

        override fun onClickNext(isPlaying: Boolean) {
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
        }

        override fun onClickPrevious(isPlaying: Boolean) {
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
    }

    private var jobUpdatePlaying: Job? = null

    private var soundVolume: Int = 0
    private var maxSoundVolume: Int = 15

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_player, container, false
        )
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        playerViewModel = ViewModelProvider(activity!!).get(PlayerViewModel::class.java)
        playerViewModel.setActivityCallback(mActivityCallback)

        stationsViewModel = ViewModelProvider(activity!!).get(StationsViewModel::class.java)
        stationsViewModel.selectedStation.observe(activity!!, Observer {
            playerViewModel.stationData.set(it)
        })

        if (stationsViewModel.selectedStation.value == null) {
            GlobalScope.launch {
                SessionRepository.get().getCurrentSession().lastRunningStationId?.let { it ->
                    RadioStationRepository.get().findStation(it)?.let { station ->
                        stationsViewModel.setSelected(
                            station,
                            updateSession = false,
                            postValue = true
                        )
                        return@launch
                    }
                }
                if (stationsViewModel.getStations().value == null) {
                    (if (stationsViewModel.playlistSelectorAny) {
                        RadioStationRepository.get().getAllStations().first()
                    } else {
                        RadioStationRepository.get().getAllFavoriteStations().first()
                    }).let {
                        stationsViewModel.setSelected(it, updateSession = true, postValue = true)
                    }
                } else {
                    stationsViewModel.getStations().value?.first()?.let {
                        stationsViewModel.setSelected(it, updateSession = true, postValue = true)
                    }
                }
            }
        }

        binding.viewModel = playerViewModel
        binding.executePendingBindings()

        (activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager).let {
            maxSoundVolume = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            setVolumeByUnits(it.getStreamVolume(AudioManager.STREAM_MUSIC))
        }

        binding.soundVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setVolumeByProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    override fun onResume() {
        super.onResume()
        jobUpdatePlaying = GlobalScope.launch {
            while (true) {
                playerViewModel.isPlaying.set(
                    SessionRepository.get().isPlaying()
                )
                Thread.sleep(100)
            }
        }
    }

    override fun onPause() {
        jobUpdatePlaying?.cancel()
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
                if (stationsViewModel.playlistSelectorAny)
                    PlayerService.EXTRA_PLAYLIST_SELECTOR_ANY
                else
                    PlayerService.EXTRA_PLAYLIST_SELECTOR_FAVORITE
            )
        })
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
}

