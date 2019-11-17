package com.p2lem8dev.internetRadio.app.ui.stations

import androidx.databinding.ObservableField
import androidx.recyclerview.widget.RecyclerView
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.databinding.StationsListItemBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class StationsListViewHolder(
    private val stationsListItemBinding: StationsListItemBinding
) :
    RecyclerView.ViewHolder(stationsListItemBinding.root) {

    private lateinit var station: RadioStation
    private lateinit var listActionHandler: ListActionHandler
    var isPlaying = ObservableField(false)

    fun bind(
        station: RadioStation,
        listActionHandler: ListActionHandler
    ) {
        this.station = station
        stationsListItemBinding.station = this.station
        stationsListItemBinding.viewHolder = this
        this.listActionHandler = listActionHandler
        stationsListItemBinding.executePendingBindings()
    }

    /**
     * Handle change favorite
     */
    fun setFavorite() {
        listActionHandler.onChangeFavorite(station)
    }

    /**
     * Handle start playing
     */
    fun setSelected() {
        listActionHandler.onSetPlay(station)
    }

    /**
     * Handle set item focused
     */
    fun focus() {

    }
}