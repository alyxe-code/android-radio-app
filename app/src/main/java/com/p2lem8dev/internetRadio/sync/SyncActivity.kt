package com.p2lem8dev.internetRadio.sync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.MainActivity
import com.p2lem8dev.internetRadio.app.ui.loader.LoaderViewModel
import com.p2lem8dev.internetRadio.databinding.ActivitySyncBinding
import com.p2lem8dev.internetRadio.net.repository.RadioRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySyncBinding
    private lateinit var syncViewModel: SyncViewModel
    private val internetTestSleepTime = 100L

    private var jobTestInternetConnection: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sync)
        syncViewModel = ViewModelProvider(this).get(SyncViewModel::class.java)
        binding.viewModel = syncViewModel
        binding.executePendingBindings()

        GlobalScope.launch {
            if (SessionRepository.get().isSyncDateValid()) {
                switchToMainActivity()
            }
            syncRadioStations {
                switchToMainActivity()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startTestInternetConnection()
    }

    override fun onPause() {
        super.onPause()
        jobTestInternetConnection?.cancel()
    }

    private fun switchToMainActivity() {
        startActivity(Intent(applicationContext, MainActivity::class.java))
        finish()
    }

    private fun syncRadioStations(onCompleted: (() -> Unit)? = null) = GlobalScope.launch {
        startTestInternetConnection()
        syncViewModel.totalCountStations.set(
            RadioRepository.get()
                .getCountStations(suspend { canContinue() })
        )
        RadioRepository.get().sync(
            onNextIteration = { _, current, total ->
                syncViewModel.currentStationIndex.set(current)
                syncViewModel.totalCountStations.set(total)
            }, canContinue = suspend { canContinue() },
            onFinish = handleOnFinish@{ _, _ ->
                jobTestInternetConnection?.cancel()
                GlobalScope.launch {
                    SessionRepository.get().updateSyncDate()
                }
                onCompleted?.invoke()
            }
        )
    }

    private fun canContinue(): Boolean {
        return syncViewModel.isNetOn.get() ?: false
    }

    private fun startTestInternetConnection() {
        jobTestInternetConnection = GlobalScope.launch(context = Dispatchers.IO) {
            val pingCommand = "ping -c 1 google.com"
            while (true) {
                val ping = Runtime.getRuntime().exec(pingCommand).waitFor()
                syncViewModel.isNetOn.set(ping == 0)
                Thread.sleep(internetTestSleepTime)
            }
        }
    }
}