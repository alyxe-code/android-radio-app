package com.p2lem8dev.internetRadio.app

import android.app.Application
import com.p2lem8dev.internetRadio.database.radio.RadioDatabase
import com.p2lem8dev.internetRadio.database.session.SessionDatabase
import com.p2lem8dev.internetRadio.net.api.RadioTochkaAPI
import com.p2lem8dev.internetRadio.net.repository.RadioRepository
import com.p2lem8dev.internetRadio.net.repository.SessionRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InternetRadioApp : Application() {

    private val mRadioTochkaAPI: RadioTochkaAPI = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(RADIO_TOCHKA_API_HOST)
        .build()
        .create(RadioTochkaAPI::class.java)

    override fun onCreate() {
        super.onCreate()

        val radioDb = RadioDatabase.getInstance(applicationContext)
        val sessionDb = SessionDatabase.getInstance(applicationContext)

        RadioRepository.create(applicationContext, radioDb.getRadioStationsDao(), mRadioTochkaAPI)
        SessionRepository.create(sessionDb.getSessionDao())
    }

    companion object {
        const val RADIO_TOCHKA_API_HOST = "https://radio-tochka.com"

    }
}