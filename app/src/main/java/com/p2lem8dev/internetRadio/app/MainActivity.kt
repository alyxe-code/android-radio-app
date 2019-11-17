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

        binding.navView.setupWithNavController(findNavController(R.id.nav_host_fragment))

        intent.getStringExtra(EXTRA_NAVIGATION_LAUNCH)?.let {
            binding.navView.selectedItemId = when (it) {
                EXTRA_NAVIGATION_HOME -> R.id.navigation_home
                EXTRA_NAVIGATION_DASHBOARD -> R.id.navigation_dashboard
                EXTRA_NAVIGATION_PLAYER -> R.id.navigation_player
                else -> R.id.navigation_home
            }
        }


        GlobalScope.launch {
            SessionRepository.get().startNewSession()
            if (!PlayerService.isPlaying()) {
                SessionRepository.get().getCurrentSession().lastRunningStationId?.let {
                    SessionRepository.get().setRadioStopped(it)
                }
            }
        }

    }

    companion object {
        const val EXTRA_NAVIGATION_LAUNCH = "extra::navigation::launch"
        const val EXTRA_NAVIGATION_HOME = "extra::navigation::home"
        const val EXTRA_NAVIGATION_DASHBOARD = "extra::navigation::dashboard"
        const val EXTRA_NAVIGATION_PLAYER = "extra::navigation::player"
    }
}
