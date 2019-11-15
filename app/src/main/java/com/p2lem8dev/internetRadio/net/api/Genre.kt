package com.p2lem8dev.internetRadio.net.api

import com.google.gson.annotations.SerializedName

class Genre(
    val genre: String,
    @SerializedName("genre_id")
    val genreId: String
)