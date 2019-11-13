package com.p2lem8dev.internetRadio.app.ui.stations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.ui.utils.ListActionHandler
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.databinding.StationsListItemBinding

class StationsListAdapter(
    private val showDetails: Boolean,
    private val listActionHandler: ListActionHandler
) : RecyclerView.Adapter<StationsListViewHolder>() {
    private var data = ArrayList<RadioStation>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationsListViewHolder {
        val binding: StationsListItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.stations_list_item,
            parent, false
        )
        return StationsListViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: StationsListViewHolder, position: Int) {
        holder.bind(data[position], showDetails, listActionHandler)
    }

    fun postData(data: List<RadioStation>) {
        this.data = data as ArrayList<RadioStation>
        notifyDataSetChanged()
    }
}

