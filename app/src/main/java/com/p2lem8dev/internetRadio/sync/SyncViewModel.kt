package com.p2lem8dev.internetRadio.sync

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

class SyncViewModel : ViewModel() {

    fun sync(
        context: Context,
        apiHostname: String,
        imagesSaveDir: String
    ) {

        GlobalScope.launch {
            try {
                RadioStationRepository.get().sync(imagesSaveDir) {
                    Log.d("SYNC", "Saving station#${it.id} ${it.title}")
                }
            } catch (e: Exception) {
                Log.wtf("SYNC", e.stackTrace.joinToString("\n"))
            }

        }

    }
}