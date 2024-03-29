package com.p2lem8dev.internetRadio.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.p2lem8dev.internetRadio.app.MainActivity
import com.p2lem8dev.internetRadio.sync.SyncActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(Intent(applicationContext, SyncActivity::class.java))
        finish()
    }
}
