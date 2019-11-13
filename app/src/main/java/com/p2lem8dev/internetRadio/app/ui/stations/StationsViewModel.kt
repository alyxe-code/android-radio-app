package com.p2lem8dev.internetRadio.app.ui.stations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class StationsViewModel : ViewModel() {

    var selectedStation = MutableLiveData<RadioStation>()
    var allStations = RadioRepository.get().getAllStationsLiveData()
    var favoriteStations = RadioRepository.get().getAllFavoriteStationsLiveData()

    private var _playlistSelectorAny: Boolean = true

    val playlistSelectorAny: Boolean
        get() = _playlistSelectorAny

    fun usePlaylist(onlyFavorite: Boolean): StationsViewModel {
        _playlistSelectorAny = onlyFavorite
        return this
    }

    fun getStations(): LiveData<List<RadioStation>> {
        return if (_playlistSelectorAny) allStations
        else favoriteStations
    }

    fun setFavoriteInvert(station: RadioStation) {
        RadioRepository.get().setFavorite(station.stationId, station.isFavorite.not())
    }

    fun setSelected(
        station: RadioStation,
        updateSession: Boolean = true,
        postValue: Boolean = false
    ) {
        findStation(station.stationId) ?: return
        if (postValue) {
            selectedStation.postValue(station)
        } else {
            selectedStation.value = station
        }
        if (updateSession) {
            GlobalScope.launch {
                SessionRepository.get().updateLastRunningStation(station.stationId)
            }
        }
    }

    private fun findStation(stationId: String): RadioStation? {
        return getStations().value?.find { it.stationId == stationId }
    }

}