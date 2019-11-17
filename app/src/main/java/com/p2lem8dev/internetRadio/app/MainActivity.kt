package com.p2lem8dev.internetRadio.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.service.player.PlayerService
import com.p2lem8dev.internetRadio.app.ui.stations.StationsViewModel
import com.p2lem8dev.internetRadio.databinding.ActivityMainBinding
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var stationsViewModel: StationsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        stationsViewModel = ViewModelProvider(this).get(StationsViewModel::class.java)

        binding.navView.let {
            it.setupWithNavController(findNavController(R.id.nav_host_fragment))
            it.selectedItemId = R.id.navigation_player
        }

        GlobalScope.launch {
            SessionRepository.get().startNewSession()
            if (!PlayerService.isPlaying()) {
                SessionRepository.get().setRadioStopped(
                    SessionRepository.get().getCurrentSession().lastRunningStationId
                )
            }
        }
    }
}
