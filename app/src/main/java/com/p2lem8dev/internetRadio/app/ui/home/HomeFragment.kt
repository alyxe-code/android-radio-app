package com.p2lem8dev.internetRadio.app.ui.home

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.ui.stations.StationsFragment
import com.p2lem8dev.internetRadio.app.ui.stations.StationsListAdapter
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import kotlinx.coroutines.*

class HomeFragment : StationsFragment(), ListActionHandler, SwipeRefreshLayout.OnRefreshListener {

    private var jobHideRefresh: Job? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        handleActivityCreated()

        setActionBarTitle(R.string.title_home)

        binding.swipeRefresh.setOnRefreshListener(this)

        stationsListAdapter = StationsListAdapter(this)
        binding.recyclerView.adapter = stationsListAdapter

        GlobalScope.launch {
            Thread.sleep(400)
            stationsViewModel.allStations.value.let {
                if (it == null || it.isEmpty()) {
                    loadStations()
                }
            }
        }
        stationsViewModel.allStations.observe(activity!!, Observer {
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

    private fun loadStations() {
        GlobalScope.launch {
            jobHideRefresh?.cancel()
            if (!stationsViewModel.isLoading) {
                stationsViewModel.loadStations(context!!, imagesSaveDirectory)
            }
            startJobHideRefresh()
        }
    }

    private fun startJobHideRefresh() {
        jobHideRefresh = GlobalScope.launch {
            if (!binding.swipeRefresh.isRefreshing) {
                withContext(context = Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = true
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

    companion object {
        const val HIDE_REFRESH_TIMEOUT = 2000L
        const val HIDE_REFRESH_STATIONS_MAX = 10
    }
}
