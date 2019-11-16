package com.p2lem8dev.internetRadio.app.utils

import android.util.Log
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class Playlist {
    private var mQueue: ArrayList<RadioStation> = arrayListOf()
    private var mCurrentIndex = 0


    val current
        get() = mQueue[mCurrentIndex]

    fun setCurrentByStationId(stationId: String): RadioStation? {
        if (isEmpty()) {
            GlobalScope.launch {
                mQueue = RadioStationRepository.get().getAllStations() as ArrayList<RadioStation>
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
            Log.d("PLAYER", "Playlist queue size = ${mQueue.size}")
            val index = if (mCurrentIndex == 0) mQueue.size - 1 else mCurrentIndex - 1
            return mQueue[index]
        }

    val size
        get() = mQueue.size

    var ready: Boolean = false

    fun isEmpty() = size == 0

    suspend fun loadAsync(
        playlistSelectorAny: Boolean,
        currentStationId: String? = null,
        then: (() -> Unit)? = null
    ) {
        ready = false

        mQueue = arrayListOf()
        if (playlistSelectorAny) {
            RadioStationRepository.get().getAllStations().let {
                mQueue = it as ArrayList<RadioStation>
            }
        } else {
            RadioStationRepository.get().getAllFavoriteStations().let {
                mQueue = it as ArrayList<RadioStation>
            }
        }

        Log.d("PLAYER_PLAYLIST", "Queue Size = $size")

        currentStationId?.let { stationId ->
            mCurrentIndex = mQueue.indexOfFirst { it.stationId == stationId }
        }

        ready = true
        then?.invoke()
    }

    companion object {
        fun createFrom(stations: List<RadioStation>, current: RadioStation? = null) =
            Playlist().apply {
                mQueue = stations as ArrayList<RadioStation>
                mCurrentIndex = if (current != null) {
                    stations.indexOfFirst { it.stationId == current.stationId }
                } else 0
            }

        fun createFrom(stations: List<RadioStation>, currentStationId: String) =
            Playlist().apply {
                mQueue = stations as ArrayList<RadioStation>
                mCurrentIndex = stations.indexOfFirst { it.stationId == currentStationId }
            }

        const val PLAYLIST_SELECTOR_ANY = true
        const val PLAYLIST_SELECTOR_FAVORITE = false
    }
}