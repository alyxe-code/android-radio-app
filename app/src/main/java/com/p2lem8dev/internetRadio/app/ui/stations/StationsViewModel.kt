package com.p2lem8dev.internetRadio.app.ui.stations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.api.BaseRadioInfo
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.*

class StationsViewModel : ViewModel() {

    var selectedStation = MutableLiveData<RadioStation>()
    var allStations = RadioStationRepository.get().getAllStationsLiveData()
    var favoriteStations = RadioStationRepository.get().getAllFavoriteStationsLiveData()

    var playingStationId: String? = null

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

    suspend fun setFavoriteInvert(station: RadioStation) {
        RadioStationRepository.get().invertFavorite(station.stationId)
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
                SessionRepository.get()
                    .updateLastRunningStation(station.stationId)
            }
        }
    }

    private fun findStation(stationId: String): RadioStation? {
        return getStations().value?.find { it.stationId == stationId }
    }

    suspend fun loadStations(imagesDownloadDirectory: String, onLoad: (suspend (BaseRadioInfo) -> Unit)? = null) =
        withContext(context = Dispatchers.IO) {
            RadioStationRepository.get().loadAllStations(
                onlyRunning = true,
                downloadImages = true,
                downloadDestinationDirectory = imagesDownloadDirectory,
                onNext = onLoad)
        }

    suspend fun getAllStations() = RadioStationRepository.get().getAllStations()

    suspend fun getAllFavoriteStations() = RadioStationRepository.get().getAllFavoriteStations()

}