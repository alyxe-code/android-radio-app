package com.p2lem8dev.internetRadio.app.utils

import android.util.Log
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Playlist {
    private var mQueue: ArrayList<RadioStation> = arrayListOf()
    private var mCurrentIndex = 0


    val current
        get() = mQueue[mCurrentIndex]

    fun setCurrentByStationId(stationId: String): RadioStation? {
        if (isEmpty()) {
            GlobalScope.launch {
                mQueue = RadioRepository.get().getAllStations() as ArrayList<RadioStation>
                setCurrentByStationId(stationId)
            }
            return null
        }
        mCurrentIndex = mQueue.indexOfFirst { it.stationId == stationId }
        return current
    }

    val next: RadioStation
        get() {
            val index = if (mCurrentIndex == mQueue.size - 1) 0 else mCurrentIndex + 1
            return mQueue[index]
        }

    val previous: RadioStation
        get() {
            val index = if (mCurrentIndex == 0) mQueue.size - 1 else mCurrentIndex -1
            return mQueue[index]
        }

    val size
        get() = mQueue.size

    var ready: Boolean = false

    fun isEmpty() = size == 0

    suspend fun loadAsync(playlistSelectorAny: Boolean, currentStationId: String? = null, then: (() -> Unit)? = null) {
        ready = false
        mQueue = if (playlistSelectorAny) RadioRepository.get().getAllStations() as ArrayList<RadioStation>
        else RadioRepository.get().getAllFavoriteStations() as ArrayList<RadioStation>

        if (currentStationId != null) {
            Log.d("PLAYER_SERVICE", "Set selected $mCurrentIndex")
            mCurrentIndex = mQueue.indexOfFirst { it.stationId == currentStationId }
        }

        ready = true
        then?.invoke()
    }

    companion object {
        fun createFrom(stations: List<RadioStation>, current: RadioStation? = null) =
            Playlist().apply {
                Log.d("PLAYER_SERVICE", "Playlist built")
                mQueue = stations as ArrayList<RadioStation>
                mCurrentIndex = if (current != null) {
                    stations.indexOfFirst { it.stationId == current.stationId }
                } else 0
            }

        fun createFrom(stations: List<RadioStation>, currentStationId: String) =
            Playlist().apply {
                Log.d("PLAYER_SERVICE", "Playlist built")
                mQueue = stations as ArrayList<RadioStation>
                mCurrentIndex = stations.indexOfFirst { it.stationId == currentStationId }
            }

        const val PLAYLIST_SELECTOR_ANY = true
        const val PLAYLIST_SELECTOR_FAVORITE = false
    }
}