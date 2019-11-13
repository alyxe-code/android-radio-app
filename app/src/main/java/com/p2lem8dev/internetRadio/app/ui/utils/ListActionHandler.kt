package com.p2lem8dev.internetRadio.app.ui.utils

import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation

interface ListActionHandler {
    fun onSelect(station: RadioStation)
    fun onChangeFavorite(station: RadioStation)
}