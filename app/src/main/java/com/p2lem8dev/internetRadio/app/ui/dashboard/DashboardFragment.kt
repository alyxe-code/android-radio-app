package com.p2lem8dev.internetRadio.app.ui.dashboard

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.ui.stations.StationsFragment
import com.p2lem8dev.internetRadio.app.ui.stations.StationsListAdapter
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.app.utils.Playlist
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DashboardFragment : StationsFragment(), ListActionHandler, SwipeRefreshLayout.OnRefreshListener {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        handleActivityCreated()

        setActionBarTitle(R.string.title_dashboard)

        binding.swipeRefresh.setOnRefreshListener(this)

        stationsListAdapter = StationsListAdapter(this)
        binding.recyclerView.adapter = stationsListAdapter

        stationsViewModel.favoriteStations.observe(activity!!, Observer {
            stationsListAdapter.postData(it)
            binding.notifyChange()
        })
    }

    override val playlistSelector: Boolean
        get() = Playlist.PLAYLIST_SELECTOR_FAVORITE

    override fun onRefresh() {
        GlobalScope.launch {
            Thread.sleep(1000)
            binding.swipeRefresh.isRefreshing = false
        }
    }

}