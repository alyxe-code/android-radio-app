package com.p2lem8dev.internetRadio.net.api

import com.google.gson.annotations.SerializedName

class FullRadioInfo(
    @SerializedName("radio_id")
    val stationId: String,
    val title: String,
    val url: String,
    val description: String,
    @SerializedName("n_views")
    val views: String,
    val enabled: String,
    @SerializedName("total_votes")
    val totalVotes: String,
    @SerializedName("total_value")
    val totalValue: String,
    @SerializedName("player_stream")
    val playerStream: String,
    val links: List<Link>,
    val language: String,
    @SerializedName("language_id")
    val languageId: String,
    val country: String,
    @SerializedName("country_id")
    val countryId: String,
    val region: String,
    @SerializedName("region_id")
    val regionId: String,
    val city: String,
    @SerializedName("city_id")
    val cityId: String,
    val genres: List<Genre>,
    val voted: String,
    val listeners: String
)