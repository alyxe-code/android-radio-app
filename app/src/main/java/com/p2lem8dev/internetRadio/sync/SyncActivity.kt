package com.p2lem8dev.internetRadio.sync

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.MainActivity
import com.p2lem8dev.internetRadio.databinding.ActivitySyncBinding
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class SyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySyncBinding
    private lateinit var syncViewModel: SyncViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sync)
        syncViewModel = ViewModelProvider(this).get(SyncViewModel::class.java)
        binding.viewModel = syncViewModel
        binding.executePendingBindings()

        GlobalScope.launch {

            if (SessionRepository.get().isSyncDateValid().not()) {
                val imagesSaveDir = filesDir.absolutePath + "/stations"
                File(imagesSaveDir).mkdir()

                RadioStationRepository.get().sync(imagesSaveDir)
            }

            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
        }
    }
}