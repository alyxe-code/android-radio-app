package com.p2lem8dev.internetRadio.app.ui.saved_stations

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.MainActivity
import com.p2lem8dev.internetRadio.app.service.player.PlayerService
import com.p2lem8dev.internetRadio.app.ui.stations.StationsFragment
import com.p2lem8dev.internetRadio.app.ui.stations.StationsListAdapter
import com.p2lem8dev.internetRadio.app.ui.stations.StationsViewModel
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.databinding.LayoutStationsBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SavedStationsFragment : StationsFragment(), ListActionHandler {

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
        return layoutStationsBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as MainActivity).let { activity ->
            activity.supportActionBar?.let {
                it.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
                it.setCustomView(R.layout.actionbar_layout)
                (it.customView.findViewById(R.id.actionbar_title) as TextView)
                    .text = getString(R.string.title_saved_stations)
            }
        }

        stationsListAdapter = StationsListAdapter(false, this)
        layoutStationsBinding.recyclerView.adapter = stationsListAdapter

        stationsViewModel = ViewModelProvider(activity!!).get(StationsViewModel::class.java)

        stationsViewModel.favoriteStations.observe(activity!!, Observer {
            stationsListAdapter.postData(it)
            layoutStationsBinding.notifyChange()
        })
    }

    override fun onSelect(station: RadioStation) {
        stationsViewModel
            .usePlaylist(Playlist.PLAYLIST_SELECTOR_FAVORITE)
            .setSelected(station)

        invokeServicePlay(
            station.stationId,
            Playlist.PLAYLIST_SELECTOR_FAVORITE
        )
    }

    override fun onChangeFavorite(station: RadioStation) {
        GlobalScope.launch {
            stationsViewModel.setFavoriteInvert(station)
        }
    }

}