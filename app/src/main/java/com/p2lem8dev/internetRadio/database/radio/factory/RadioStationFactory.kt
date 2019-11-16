package com.p2lem8dev.internetRadio.database.radio.factory

import com.p2lem8dev.internetRadio.database.radio.entities.RadioStation
import com.p2lem8dev.internetRadio.net.api.BaseRadioInfo
import com.p2lem8dev.internetRadio.net.api.FullRadioInfo

class RadioStationFactory {

    companion object {
        fun from(
            radio: FullRadioInfo,
            isFavorite: Boolean = false,
            imageUrl: String? = null
        ): RadioStation {
            return RadioStation(
                stationId = radio.stationId,
                title = radio.title,
                country = radio.country,
                region = radio.region,
                city = radio.city,
                links = radio.links.map { it.url } as ArrayList<String>,
                listeners = radio.listeners.toInt(),
                views = radio.views.toInt(),
                playerStream = radio.playerStream,
                language = radio.language,
                genres = radio.genres.map { it.genre } as ArrayList<String>,
                voted = radio.voted.toInt(),
                isFavorite = isFavorite,
                imageUrl = imageUrl
            )
        }

        fun fromBaseInfo(
            radio: BaseRadioInfo,
            isFavorite: Boolean = false,
            imageUrl: String? = null
        ): RadioStation {
            return RadioStation(
                stationId = radio.id,
                title = radio.title,
                links = radio.links.map { it.url } as ArrayList<String>,
                genres = arrayListOf()
            )
        }
    }
}