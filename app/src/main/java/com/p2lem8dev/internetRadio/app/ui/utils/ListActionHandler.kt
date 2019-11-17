package com.p2lem8dev.internetRadio.app.ui.utils

import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation

interface ListActionHandler {

    /**
     * When click to play station
     */
    fun onSetPlay(station: RadioStation)

    /**
     * When click to change favorite state
     */
    fun onChangeFavorite(station: RadioStation)
}