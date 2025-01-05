//package com.example.weather
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//
//class WeatherAdapter(private val dailyWeatherList: List<DailyWeather>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//    companion object {
//        private const val TYPE_HEADER = 0
//        private const val TYPE_ITEM = 1
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        // Определяем тип элемента: заголовок (HEADER) или элемент (ITEM)
//        return if (isHeader(position)) TYPE_HEADER else TYPE_ITEM
//    }
//
//    private fun isHeader(position: Int): Boolean {
//        // Возвращаем true, если это заголовок (например, каждый первый элемент в списке DailyWeather)
//        var itemCount = 0
//        for (daily in dailyWeatherList) {
//            if (position == itemCount) return true
//            itemCount += daily.forecasts.size + 1 // +1 для заголовка
//        }
//        return false
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return if (viewType == TYPE_HEADER) {
//            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
//            DateViewHolder(view)
//        } else {
//            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weather_detail, parent, false)
//            WeatherViewHolder(view)
//        }
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        if (holder is DateViewHolder) {
//            holder.bind(getDateForPosition(position))
//        } else if (holder is WeatherViewHolder) {
//            holder.bind(getDetailForPosition(position))
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return dailyWeatherList.sumOf { it.forecasts.size + 1 } // +1 для заголовка каждого дня
//    }
//
//    private fun getDateForPosition(position: Int): String {
//        var currentPos = 0
//        for (daily in dailyWeatherList) {
//            if (position == currentPos) return daily.date
//            currentPos += daily.forecasts.size + 1
//        }
//        return ""
//    }
//
//    private fun getDetailForPosition(position: Int): WeatherDetail {
//        var currentPos = 0
//        for (daily in dailyWeatherList) {
//            currentPos++ // Пропускаем заголовок
//            for (detail in daily.forecasts) {
//                if (position == currentPos) return detail
//                currentPos++
//            }
//        }
//        throw IllegalArgumentException("Invalid position")
//    }
//
//    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val dateText: TextView = itemView.findViewById(R.id.dateText)
//
//        fun bind(date: String) {
//            dateText.text = date
//        }
//    }
//
//    inner class WeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val icon: ImageView = itemView.findViewById(R.id.conditionIcon)
//        private val maxTempText: TextView = itemView.findViewById(R.id.maxTempText)
//        private val minTempText: TextView = itemView.findViewById(R.id.minTempText)
//
//        fun bind(detail: WeatherDetail) {
//            Glide.with(itemView.context).load(detail.conditionIcon).into(icon)
//            maxTempText.text = "Max: ${detail.maxTemp}°C"
//            minTempText.text = "Min: ${detail.minTemp}°C"
//        }
//    }
//}
