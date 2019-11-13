package com.p2lem8dev.internetRadio.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.service.player.PlayerService
import com.p2lem8dev.internetRadio.app.ui.stations.StationsFragment
import com.p2lem8dev.internetRadio.app.ui.stations.StationsListAdapter
import com.p2lem8dev.internetRadio.app.ui.stations.StationsViewModel
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.databinding.LayoutStationsBinding

class HomeFragment : StationsFragment(), ListActionHandler {
    private lateinit var layoutStationsBinding: LayoutStationsBinding
    private lateinit var stationsListAdapter: StationsListAdapter
    private lateinit var stationsViewModel: StationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        layoutStationsBinding = DataBindingUtil.inflate(
            inflater, R.layout.layout_stations, container, false
        )
        layoutStationsBinding.stationsAmount = 0
        layoutStationsBinding.pageName = "Home"
        return layoutStationsBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        stationsListAdapter = StationsListAdapter(false, this)
        layoutStationsBinding.recyclerView.adapter = stationsListAdapter

        stationsViewModel = ViewModelProvider(activity!!).get(StationsViewModel::class.java)

        stationsViewModel.allStations.observe(activity!!, Observer {
            stationsListAdapter.postData(it)
            layoutStationsBinding.stationsAmount = it.size
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
        stationsViewModel.setFavoriteInvert(station)
    }


}