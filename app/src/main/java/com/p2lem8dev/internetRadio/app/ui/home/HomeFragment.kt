package com.p2lem8dev.internetRadio.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.InternetRadioApp
import com.p2lem8dev.internetRadio.app.ui.stations.StationsFragment
import com.p2lem8dev.internetRadio.app.ui.stations.StationsListAdapter
import com.p2lem8dev.internetRadio.app.ui.stations.StationsViewModel
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.databinding.LayoutStationsBinding
import kotlinx.coroutines.*

class HomeFragment : StationsFragment(), ListActionHandler, SwipeRefreshLayout.OnRefreshListener {
        private lateinit var layoutStationsBinding: LayoutStationsBinding
    private lateinit var stationsListAdapter: StationsListAdapter
    private lateinit var stationsViewModel: StationsViewModel

    private var jobStopRefresh: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        layoutStationsBinding = DataBindingUtil.inflate(
            inflater, R.layout.layout_stations, container, false
        )
        layoutStationsBinding.swipeRefresh.setOnRefreshListener(this)
        return layoutStationsBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        stationsListAdapter = StationsListAdapter(false, this)
        layoutStationsBinding.recyclerView.adapter = stationsListAdapter

        stationsViewModel = ViewModelProvider(activity!!).get(StationsViewModel::class.java)

        GlobalScope.launch {
            Thread.sleep(400)
            stationsViewModel.allStations.value.let {
                if (it == null || it.isEmpty()) {
                    startStopRefreshJob(false)
                    stationsViewModel.loadStations(getImagesSaveDirectory())
                }
            }
        }
        stationsViewModel.allStations.observe(activity!!, Observer {
            if (it.size > MAX_STATIONS_MUST_BE_LOADED) {
                jobStopRefresh?.cancel()
            }
            stationsListAdapter.postData(it)
            layoutStationsBinding.notifyChange()
        })
    }

    override fun onSelect(station: RadioStation) {
        stationsViewModel
            .usePlaylist(Playlist.PLAYLIST_SELECTOR_ANY)
            .setSelected(station)

        invokeServicePlay(
            station.stationId,
            Playlist.PLAYLIST_SELECTOR_ANY
        )
    }

    override fun onChangeFavorite(station: RadioStation) {
        GlobalScope.launch {
            stationsViewModel.setFavoriteInvert(station)
        }
    }

    override fun onRefresh() {
        GlobalScope.launch {
            startStopRefreshJob(true)
            stationsViewModel.loadStations(getImagesSaveDirectory())
        }
    }

    private suspend fun startStopRefreshJob(isRefreshing: Boolean) {
        if (!isRefreshing) {
            withContext(context = Dispatchers.Main) {
                layoutStationsBinding.swipeRefresh.isRefreshing = true
            }
        }
        jobStopRefresh = GlobalScope.launch {
            while (true) {
                stationsViewModel.allStations.value.let {
                    if (it != null && it.size > MAX_STATIONS_MUST_BE_LOADED) {
                        withContext(context = Dispatchers.Main) {
                            layoutStationsBinding.swipeRefresh.isRefreshing = false
                        }
                        jobStopRefresh?.cancel()
                    }
                }
            }
        }
        jobStopRefresh?.start()
    }


    private fun getImagesSaveDirectory() =
        (activity!!.application as InternetRadioApp).getImagesSaveDirectory()

    companion object {
        const val MAX_STATIONS_MUST_BE_LOADED = 6
    }

}
