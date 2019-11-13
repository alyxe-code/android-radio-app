package com.p2lem8dev.internetRadio.database.radio.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.collections.ArrayList

@Entity(tableName = "radio_stations")
class RadioStation (
    val stationId: String,
    var title: String,
    var country: String? = null,
    var region: String? = null,
    var city: String? = null,
    var links: ArrayList<String>,
    var listeners: Int = 0,
    var views: Int = 0,
    var playerStream: String? = null,
    var language: String,
    var genres: ArrayList<String>,
    var voted: Int = 0,
    var isFavorite: Boolean = false,
    var imageUrl: String? = null
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}