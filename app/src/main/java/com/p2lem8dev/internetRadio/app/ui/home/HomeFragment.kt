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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        handleActivityCreated()

        setActionBarTitle(R.string.title_home)

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
}
