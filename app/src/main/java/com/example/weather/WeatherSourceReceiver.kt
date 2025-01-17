package com.example.weather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WeatherSourceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val source = intent?.getStringExtra("source")
        // Здесь вы можете обработать полученный источник
    }
}

