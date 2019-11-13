package com.p2lem8dev.internetRadio.app.ui.player

import android.util.Log
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation

class PlayerViewModel : ViewModel() {

    var stationData: ObservableField<RadioStation> = ObservableField()
    val isPlaying = ObservableField<Boolean>(false)

    interface ActivityCallback {
        fun onClickNext(isPlaying: Boolean)
        fun onClickPrevious(isPlaying: Boolean)
        fun onClickPlayStop(isPlaying: Boolean)
    }

    private var mActivityCallback: ActivityCallback? = null

    fun setActivityCallback(activityCallback: ActivityCallback) {
        mActivityCallback = activityCallback
    }

    fun handleClickNext() {
        mActivityCallback?.onClickNext(isPlaying.get()!!)
    }

    fun handleClickPrevious() {
        mActivityCallback?.onClickPrevious(isPlaying.get()!!)
    }

    fun handleClickPlayStop() {
        Log.d("PLAYER_TEST", "CLICK PlayStop")
        mActivityCallback?.onClickPlayStop(isPlaying.get()!!)
    }

    fun hasNext(): Boolean {
        return true
    }

    fun hasPrevious(): Boolean {
        return true
    }

}

