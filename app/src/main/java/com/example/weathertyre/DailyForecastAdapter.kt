package com.example.weathertyre

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.round
class DailyForecastAdapter(
    private val forecasts: List<DailyForecast>,
    private val temperatureUnit: String // Добавляем параметр для единицы измерения
) : RecyclerView.Adapter<DailyForecastAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        val maxTempText: TextView = view.findViewById(R.id.maxTempText)
        val minTempText: TextView = view.findViewById(R.id.minTempText)
        val conditionIcon: ImageView = view.findViewById(R.id.conditionIcon)
        val dateTextLabel: TextView = view.findViewById(R.id.dateTextLabel)
        val maxTempLabel: TextView = view.findViewById(R.id.maxTempLabel)
        val minTempLabel: TextView = view.findViewById(R.id.minTempLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_daily_forecast, parent, false)
        return ViewHolder(view)
    }

    private fun convertTemperature(celsius: Double): Double {
        return if (temperatureUnit == "F") {
            celsius * 9/5 + 32 // Конвертация в Фаренгейты
        } else {
            celsius // Оставляем Цельсии
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val forecast = forecasts[position]

        // Форматирование даты
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = dateFormat.parse(forecast.date)

        val displayFormat = SimpleDateFormat("d MMMM", Locale.getDefault())
        val displayDate = parsedDate?.let { displayFormat.format(it) }

        // Определение "сегодня", "завтра", "послезавтра"
        val calendar = Calendar.getInstance()
        val today = calendar.clone() as Calendar
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = calendar.clone() as Calendar
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrow = calendar.clone() as Calendar

        val dateCalendar = Calendar.getInstance()
        if (parsedDate != null) {
            dateCalendar.time = parsedDate
        }

        val context = holder.itemView.context
        val dateText = when {
            isSameDay(today, dateCalendar) -> context.getString(R.string.today)
            isSameDay(tomorrow, dateCalendar) -> context.getString(R.string.tomorrow)
            isSameDay(dayAfterTomorrow, dateCalendar) -> context.getString(R.string.day_after_tomorrow)
            else -> displayDate
        }

        holder.dateText.text = dateText

        // Конвертация и округление температуры
        val maxTemp = convertTemperature(forecast.maxTemp)
        val minTemp = convertTemperature(forecast.minTemp)

        // Форматирование строк температуры
        holder.maxTempText.text = "${round(maxTemp).toInt()} °$temperatureUnit"
        holder.minTempText.text = "${round(minTemp).toInt()} °$temperatureUnit"

        Glide.with(holder.itemView.context)
            .load("https:${forecast.conditionIcon}")
            .into(holder.conditionIcon)

        // Условное отображение подписей только для первого элемента
        if (position == 0) {
            holder.dateTextLabel.visibility = View.VISIBLE
            holder.maxTempLabel.visibility = View.VISIBLE
            holder.minTempLabel.visibility = View.VISIBLE
        } else {
            holder.dateTextLabel.visibility = View.GONE
            holder.maxTempLabel.visibility = View.GONE
            holder.minTempLabel.visibility = View.GONE
        }
    }

    override fun getItemCount() = forecasts.size

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}