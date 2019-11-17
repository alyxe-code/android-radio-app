package com.p2lem8dev.internetRadio.app.ui.stations

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.InternetRadioApp
import com.p2lem8dev.internetRadio.app.service.player.PlayerService
import com.p2lem8dev.internetRadio.app.ui.home.HomeFragment
import com.p2lem8dev.internetRadio.app.ui.utils.BindingFragment
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.databinding.LayoutStationsBinding
import kotlinx.coroutines.*

open class StationsFragment : BindingFragment<LayoutStationsBinding>(R.layout.layout_stations), ListActionHandler {

    protected lateinit var stationsViewModel: StationsViewModel
    protected lateinit var stationsListAdapter: StationsListAdapter

    protected fun setActionBarTitle(titleId: Int) = setActionBarTitle(getString(titleId))

    fun handleActivityCreated() {
        stationsViewModel = ViewModelProvider(activity!!).get(StationsViewModel::class.java)
    }

    protected fun invokeServicePlay(stationId: String, playlistSelector: Boolean) {
        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_STATION_ID, stationId)
            putExtra(
                PlayerService.EXTRA_PLAYLIST_SELECTOR,
                playlistSelector
            )
        }
        activity?.startForegroundService(intent)
    }

    protected fun setActionBarTitle(title: String) {
        activity?.let { activity ->
            (activity as AppCompatActivity).let { appCompatActivity ->
                appCompatActivity.supportActionBar?.let { actionbar ->
                    actionbar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
                    actionbar.setCustomView(R.layout.actionbar_layout)
                    actionbar.customView
                        .findViewById<TextView>(R.id.actionbar_title)
                        .text = title
                }

            }
        }
    }

    open val playlistSelector: Boolean
        get() = Playlist.PLAYLIST_SELECTOR_ALL

    open val imagesSaveDirectory: String
        get() = (activity?.application as InternetRadioApp)
            .getImagesSaveDirectory()

    open override fun onSetPlay(station: RadioStation) {
        stationsViewModel
            .usePlaylist(playlistSelector)
            .setSelected(station)

        invokeServicePlay(
            station.stationId,
            playlistSelector
        )
    }

    open override fun onChangeFavorite(station: RadioStation) {
        GlobalScope.launch { stationsViewModel.setFavoriteInvert(station) }
    }

    protected var jobHideRefresh: Job? = null

    protected open fun loadStations() {
        GlobalScope.launch {
            jobHideRefresh?.cancel()
            if (!stationsViewModel.isLoading) {
                stationsViewModel.loadStations(context!!, imagesSaveDirectory)
            }
            startJobHideRefresh()
        }
    }

    protected open fun startJobHideRefresh() {
        jobHideRefresh = GlobalScope.launch {
            if (!binding.swipeRefresh.isRefreshing) {
                withContext(context = Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = true
                }
            }
            while (true) {
                if (stationsViewModel.allStations.value != null &&
                    stationsViewModel.allStations.value!!.size >= HIDE_REFRESH_STATIONS_MAX
                ) {
                    break
                }
                Thread.sleep(HIDE_REFRESH_TIMEOUT / 10)
                break
            }
            stationsViewModel.isLoading = false
        }
    }

    companion object {
        const val HIDE_REFRESH_TIMEOUT = 2000L
        const val HIDE_REFRESH_STATIONS_MAX = 10
        const val SYNC_TEST_TIMEOUT = 2000L
    }
}