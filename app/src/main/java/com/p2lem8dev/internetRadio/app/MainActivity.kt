package com.p2lem8dev.internetRadio.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.p2lem8dev.internetRadio.R
import com.p2lem8dev.internetRadio.app.ui.stations.StationsViewModel
import com.p2lem8dev.internetRadio.databinding.ActivityMainBinding
import com.p2lem8dev.internetRadio.net.repository.RadioRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mStationsViewModel: StationsViewModel
//    private var jobUpdateSelectedStation: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mStationsViewModel = ViewModelProvider(this).get(StationsViewModel::class.java)

//        jobUpdateSelectedStation = GlobalScope.launch {
//            while (true) {
//                SessionRepository.get().getCurrentSession().let {
//                    if (it.lastRunningStationId != null && mStationsViewModel.selectedStation.value != null &&
//                        it.lastRunningStationId != mStationsViewModel.selectedStation.value!!.stationId
//                    ) {
//                        mStationsViewModel.setSelected(
//                            RadioRepository.get()
//                                .findByStationId(it.lastRunningStationId!!)!!,
//                            updateSession = false,
//                            postValue = true
//                        )
//                    }
//                }
//                Thread.sleep(100)
//            }
//        }

        binding.navView.setupWithNavController(findNavController(R.id.nav_host_fragment))
    }

}
