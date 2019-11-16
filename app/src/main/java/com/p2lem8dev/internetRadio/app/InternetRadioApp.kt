package com.p2lem8dev.internetRadio.app

import android.app.Application
import com.p2lem8dev.internetRadio.database.radio.RadioDatabase
import com.p2lem8dev.internetRadio.database.session.SessionDatabase
import com.p2lem8dev.internetRadio.net.api.RadioAPI
import com.p2lem8dev.internetRadio.net.repository.RadioStationRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InternetRadioApp : Application() {

    private val mRadioAPI: RadioAPI = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(RADIO_TOCHKA_API_HOST)
        .build()
        .create(RadioAPI::class.java)

    override fun onCreate() {
        super.onCreate()

        val radioDb = RadioDatabase.getInstance(applicationContext)
        val sessionDb = SessionDatabase.getInstance(applicationContext)

        RadioStationRepository.create(
            applicationContext,
            radioDb.getRadioStationsDao(),
            mRadioAPI
        )
        SessionRepository.create(sessionDb.getSessionDao())
    }

    fun getImagesSaveDirectory(): String {
        return "${filesDir.absolutePath}/images/"
    }

    companion object {
        const val RADIO_TOCHKA_API_HOST = "https://radio-tochka.com"
    }
}