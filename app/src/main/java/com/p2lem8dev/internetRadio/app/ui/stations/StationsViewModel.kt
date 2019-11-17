package com.p2lem8dev.internetRadio.app.ui.stations

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.p2lem8dev.internetRadio.app.service.sync.SyncService
import com.p2lem8dev.internetRadio.app.utils.Playlist
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.*

class StationsViewModel : ViewModel() {

    private var _selectedStation = MutableLiveData<RadioStation>()
    val selectedStation: MutableLiveData<RadioStation>
        get() {
            if (_selectedStation.value == null) {
                loadSelectedStation()
            }
            return _selectedStation
        }

    var allStations = RadioStationRepository.get().getAllStationsLiveData()
    var favoriteStations = RadioStationRepository.get().getAllFavoriteStationsLiveData()

    private var _playlistSelector: Boolean = true

    var playlistSelector: Boolean
        get() = _playlistSelector
        private set(value) {
            _playlistSelector = value
        }

    var isLoading = false

    private fun loadSelectedStation() = GlobalScope.launch(context = Dispatchers.IO) {
        if (_selectedStation.value != null) {
            return@launch
        } else {
            SessionRepository.get().getCurrentSession().lastRunningStationId?.let { id ->
                RadioStationRepository.get().findStation(id)?.let { station ->
                    _selectedStation.postValue(station)
                }
                return@launch
            }

            val station = getFirstStation()
            SessionRepository.get().let { sessionRepository ->
                sessionRepository.updateLastRunningStation(station.stationId)
                _selectedStation.postValue(station)
            }
        }
    }

    fun usePlaylist(onlyFavorite: Boolean): StationsViewModel {
        playlistSelector = if (onlyFavorite) {
            Playlist.PLAYLIST_SELECTOR_FAVORITE
        } else {
            Playlist.PLAYLIST_SELECTOR_ALL
        }
        return this
    }

    fun getStations(): LiveData<List<RadioStation>> {
        return when (playlistSelector) {
            Playlist.PLAYLIST_SELECTOR_FAVORITE -> favoriteStations
            else -> allStations
        }
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
            _selectedStation.postValue(station)
        } else {
            _selectedStation.value = station
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

    fun loadStations(
        context: Context,
        imagesDownloadDirectory: String,
        onLoad: (() -> Unit)? = null
    ) = SyncService.start(context, imagesDownloadDirectory) {
        onLoad?.invoke()
    }

    suspend fun getAllStations() = RadioStationRepository.get().getAllStations()

    suspend fun getAllFavoriteStations() = RadioStationRepository.get().getAllFavoriteStations()

    suspend fun getFirstStation() = RadioStationRepository.get().getAllStations().first()

}