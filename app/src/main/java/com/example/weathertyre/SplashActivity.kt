package com.example.weathertyre

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Логика для перехода на MainActivity или другую активность
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}