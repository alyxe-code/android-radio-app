package com.p2lem8dev.internetRadio.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.InternetRadioApp
import com.p2lem8dev.internetRadio.app.MainActivity
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

    private var countStationsLoaded: Int = 0

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

        (activity as MainActivity).let { activity ->
            activity.supportActionBar?.let {
                it.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
                it.setCustomView(R.layout.actionbar_layout)
                (it.customView.findViewById(R.id.actionbar_title) as TextView)
                    .text = getString(R.string.title_home)
            }
        }

        stationsListAdapter = StationsListAdapter(false, this)
        layoutStationsBinding.recyclerView.adapter = stationsListAdapter

        stationsViewModel = ViewModelProvider(activity!!).get(StationsViewModel::class.java)

        GlobalScope.launch {
            Thread.sleep(400)
            stationsViewModel.allStations.value.let {
                if (it == null || it.isEmpty()) {
                    loadStations()
                }
            }
        }
        stationsViewModel.allStations.observe(activity!!, Observer {
            countStationsLoaded = it.size
            stationsListAdapter.postData(it)
            layoutStationsBinding.notifyChange()
        })
    }

    override fun onPause() {
        super.onPause()
        jobHideRefresh?.cancel()
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
        loadStations()
    }

    private fun loadStations() {
        GlobalScope.launch {
            jobHideRefresh?.cancel()
            if (!stationsViewModel.isLoading) {
                stationsViewModel.loadStations(context!!, getImagesSaveDirectory())
            }
            startJobHideRefresh()
        }
    }

    private var jobHideRefresh: Job? = null

    private fun startJobHideRefresh() {
        jobHideRefresh = GlobalScope.launch {
            if (!layoutStationsBinding.swipeRefresh.isRefreshing) {
                withContext(context = Dispatchers.Main) {
                    layoutStationsBinding.swipeRefresh.isRefreshing = true
                }
            }
            while (true) {
                if (stationsViewModel.allStations.value != null &&
                    stationsViewModel.allStations.value!!.size >= HIDE_REFRESH_STATIONS_MAX) {
                    break
                }
                Thread.sleep(HIDE_REFRESH_TIMEOUT / 10)
                break
            }
            stationsViewModel.isLoading = false
        }
    }


    private fun getImagesSaveDirectory() =
        (activity!!.application as InternetRadioApp).getImagesSaveDirectory()


    companion object {
        const val HIDE_REFRESH_TIMEOUT = 2000L
        const val HIDE_REFRESH_STATIONS_MAX = 10
    }
}
