package com.p2lem8dev.internetRadio.app.utils

import android.util.Log
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Playlist {

    private var mQueue: ArrayList<RadioStation> = arrayListOf()
    private var mCurrentIndex = 0


    val current
        get() = mQueue[mCurrentIndex]

    fun setCurrentByStationId(stationId: String): RadioStation? {
        if (isEmpty()) {
            GlobalScope.launch {
                withContext(context = Dispatchers.IO) {
                    mQueue = RadioStationRepository.get()
                        .getAllStations() as ArrayList<RadioStation>
                }
                withContext(context = Dispatchers.Main) {
                    setCurrentByStationId(stationId)
                }
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
        playlistSelectorAll: Boolean,
        currentStationId: String? = null,
        then: (() -> Unit)? = null
    ) {
        ready = false
        mQueue = arrayListOf()

        mQueue = if (playlistSelectorAll) {
            RadioStationRepository.get().getAllStations()
        } else {
            RadioStationRepository.get().getAllFavoriteStations()
        } as ArrayList<RadioStation>

        currentStationId?.let { stationId ->
            mCurrentIndex = mQueue.indexOfFirst { it.stationId == stationId }
        }

        ready = true
        then?.invoke()
    }

    fun removeByStationId(stationId: String): RadioStation {
        var idx = mQueue.indexOfFirst { it.stationId == stationId }
        mQueue.removeAt(idx)

        if (idx >= mQueue.size) {
            idx = 0
        }

        if (idx < 0) {
            idx = 0
        }

        mCurrentIndex = idx
        return current
    }

    companion object {

        const val PLAYLIST_SELECTOR_ALL = true
        const val PLAYLIST_SELECTOR_FAVORITE = false

        fun createFrom(stations: List<RadioStation>, current: RadioStation? = null) =
            createFrom(stations, current?.stationId)

        fun createFrom(stations: List<RadioStation>, currentStationId: String?) =
            Playlist().apply {
                mQueue = stations as ArrayList<RadioStation>
                mCurrentIndex = if (currentStationId != null) {
                    stations.indexOfFirst { it.stationId == currentStationId }
                } else 0
            }
    }
}