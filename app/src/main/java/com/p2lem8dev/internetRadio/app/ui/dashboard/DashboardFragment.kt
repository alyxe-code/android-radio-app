package com.p2lem8dev.internetRadio.app.ui.dashboard

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.ui.stations.StationsFragment
import com.p2lem8dev.internetRadio.app.ui.stations.StationsListAdapter
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DashboardFragment : StationsFragment(), ListActionHandler,
    SwipeRefreshLayout.OnRefreshListener {

    override val playlistSelector: Boolean
        get() = Playlist.PLAYLIST_SELECTOR_FAVORITE

    private val allStationsObserver = Observer<List<RadioStation>> {
        onceAllStationsLoaded()
    }

    private fun onceAllStationsLoaded() {
        stationsViewModel.allStations.removeObserver(allStationsObserver)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        handleActivityCreated()

        setActionBarTitle(R.string.title_dashboard)

        binding.swipeRefresh.setOnRefreshListener(this)

        stationsListAdapter = StationsListAdapter(this)
        binding.recyclerView.adapter = stationsListAdapter

        GlobalScope.launch(context = Dispatchers.IO) {
            Thread.sleep(SYNC_TEST_TIMEOUT)
            stationsViewModel.allStations.value.let {
                if (it == null || it.isEmpty()) {
                    loadStations()
                }
            }
        }

        stationsViewModel.allStations.observe(activity!!, allStationsObserver)

        stationsViewModel.favoriteStations.observe(activity!!, Observer {
            stationsListAdapter.postData(it)
            binding.notifyChange()
        })
    }

    override fun onPause() {
        super.onPause()
        jobHideRefresh?.cancel()
    }

    override fun onRefresh() {
        loadStations()
    }

}