package com.p2lem8dev.internetRadio.net.api

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
    ): List<BaseRadioInfo>

    @GET("/radio/catalog/get_radio.php")
    suspend fun getRadio(@Query("r") stationId: String): FullRadioInfo?

    @GET("/radio/catalog/show_img.php")
    suspend fun getStationImage(@Query("id") stationId: String): ResponseBody

    @GET("/radio/catalog/show_img2.php")
    suspend fun getStationImage2(@Query("id") stationId: String): ResponseBody
}

