package com.p2lem8dev.internetRadio.app.ui.stations

import androidx.recyclerview.widget.RecyclerView
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.databinding.StationsListItemBinding

class StationsListViewHolder(
    private val stationsListItemBinding: StationsListItemBinding
) :
    RecyclerView.ViewHolder(stationsListItemBinding.root) {

    private lateinit var station: RadioStation
    private lateinit var listActionHandler: ListActionHandler

    fun bind(station: RadioStation, showDetails: Boolean, listActionHandler: ListActionHandler) {
        this.station = station
        stationsListItemBinding.station = this.station
        stationsListItemBinding.showDetails = showDetails
        stationsListItemBinding.viewHolder = this
        stationsListItemBinding.executePendingBindings()
        this.listActionHandler = listActionHandler
    }

    fun setFavorite() {
        listActionHandler.onChangeFavorite(station)
    }

    fun setSelected() {
        listActionHandler.onSelect(station)
    }
}