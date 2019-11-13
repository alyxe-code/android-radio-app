package com.p2lem8dev.internetRadio.net.api

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

// https://radio-tochka.com
interface RadioTochkaAPI {

    @GET("/radio/catalog/pages.php")
    suspend fun getCountRadios(): RadiosCountResponse

    @GET("/radio/catalog/get_radios2.php")
    suspend fun getPage(
        @Query("p") page: Int,
        @Query("sort") sort: String = "val",
        @Query("search") search: String = ""
    ): List<RadioBaseResponse>

    @GET("/radio/catalog/get_radio.php")
    suspend fun getRadio(@Query("r") stationId: String): RadioDataResponse?

    @GET("/radio/catalog/show_img.php")
    suspend fun getStationThumb(@Query("id") stationId: String): ResponseBody

    @GET("/radio/catalog/show_img2.php")
    suspend fun getStationImage(@Query("id") stationId: String): ResponseBody
}

data class RadiosCountResponse(
    @SerializedName("n_pages")
    val items: String
)

class RadioBaseResponse(
    val id: String,
    val title: String,
    val links: List<Link>
)

data class Link(
    val url: String,
    val bitrate: String,
    val app: String
)

data class Genre(
    val genre: String,
    @SerializedName("genre_id")
    val genreId: String
)

class RadioDataResponse(
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