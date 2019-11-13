package com.p2lem8dev.internetRadio.app.ui.stations

import android.content.Intent
import androidx.fragment.app.Fragment
import com.p2lem8dev.internetRadio.app.service.player.PlayerService

open class StationsFragment : Fragment() {

    protected fun invokeServicePlay(stationId: String, playlistSelector: Boolean) {
        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_STATION_ID, stationId)
            putExtra(
                PlayerService.EXTRA_PLAYLIST_SELECTOR,
                playlistSelector
            )
        }
        activity?.startForegroundService(intent)
    }
}