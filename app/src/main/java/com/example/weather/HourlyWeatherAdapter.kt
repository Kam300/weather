package com.example.weather

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HourlyWeatherAdapter(private val hourlyData: List<HourlyWeather>) :
    RecyclerView.Adapter<HourlyWeatherAdapter.HourViewHolder>() {

    class HourViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
        val temperatureTextView: TextView = view.findViewById(R.id.temperatureTextView)
        val iconImageView: ImageView = view.findViewById(R.id.iconImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_weather, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        val hourData = hourlyData[position]
        val time = hourData.time
        val hour = time.substring(11, 13) // Извлекаем только час
        holder.timeTextView.text = "$hour:00" // Отображаем час в формате "HH:00"
        holder.temperatureTextView.text = "${hourData.temperature} °C"


        Glide.with(holder.itemView.context)
            .load("https:${hourData.iconUrl}")
            .into(holder.iconImageView)
    }

    override fun getItemCount() = hourlyData.size
}