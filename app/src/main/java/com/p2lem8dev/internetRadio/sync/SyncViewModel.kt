package com.p2lem8dev.internetRadio.sync

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job

class SyncViewModel : ViewModel() {
    val totalCountStations = ObservableField(0)
    val currentStationIndex = ObservableField(0)
    val isNetOn = ObservableField(false)
}