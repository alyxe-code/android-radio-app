package com.p2lem8dev.internetRadio.sync

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.p2lem8dev.internetRadio.app.service.sync.SyncService
import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    fun sync() {

        val syncService = SyncService.getInstance()
        if (syncService != null) {
            Log.d("SYNC_BG_FG", "Service is working")
        } else {
            SyncService.start(getApplication<Application>().applicationContext)
        }

    }
}