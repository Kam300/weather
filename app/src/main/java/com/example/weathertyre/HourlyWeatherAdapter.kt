package com.example.weathertyre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HourlyWeatherAdapter(
    private val hourlyData: List<HourlyWeather>,
    private val temperatureUnit: String // Добавляем параметр для единицы измерения
) : RecyclerView.Adapter<HourlyWeatherAdapter.HourViewHolder>() {

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
        holder.timeTextView.text = "$hour:00"

        // Конвертируем температуру в зависимости от выбранной единицы измерения
        val temperature = if (temperatureUnit == "F") {
            hourData.temperature * 9/5 + 32 // Конвертация в Фаренгейты
        } else {
            hourData.temperature // Оставляем Цельсии
        }

        // Форматируем строку температуры с округлением до одного знака после запятой
        holder.temperatureTextView.text = String.format("%.1f °%s", temperature, temperatureUnit)

        Glide.with(holder.itemView.context)
            .load("https:${hourData.iconUrl}")
            .into(holder.iconImageView)
    }

    override fun getItemCount() = hourlyData.size
}