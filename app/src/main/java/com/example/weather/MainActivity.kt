package com.example.weathertyre

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import kotlin.concurrent.thread
import com.bumptech.glide.Glide
import android.widget.ListView
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.weather.DailyForecastAdapter
import com.example.weathertyre.databinding.ActivityMainBinding


import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private lateinit var fusedLocationClient: FusedLocationProviderClient
private val currentVersion = "1.1.0"

class MainActivity : AppCompatActivity() {
    private lateinit var cityText: TextView
    private var currentCityName: String = "" // Переменная для хранения текущего города
    private lateinit var recommendationText: TextView
    private lateinit var weatherImage: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var lastUpdatedText: TextView
    private lateinit var information: ImageButton

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var bannerAd: BannerAdView? = null
    private lateinit var binding: ActivityMainBinding

    private val adSize: BannerAdSize
        get() {
            // Calculate the width of the ad, taking into account the padding in the ad container.
            var adWidthPixels = binding.banner.width
            if (adWidthPixels == 0) {
                // If the ad hasn't been laid out, default to the full screen width
                adWidthPixels = resources.displayMetrics.widthPixels
            }
            val adWidth = (adWidthPixels / resources.displayMetrics.density).roundToInt()

            return BannerAdSize.stickySize(this, adWidth)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        setLocale(getSavedLanguage()) // Set the language before super
        super.onCreate(savedInstanceState)

        // Инициализация View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.banner.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.banner.viewTreeObserver.removeOnGlobalLayoutListener(this)
                bannerAd = loadBannerAd(adSize)
            }
        })

        MobileAds.initialize(this) {
            println("YandexAds initialized")
        }

        enableEdgeToEdge()

        // Инициализация ваших компонентов
        cityText = findViewById(R.id.cityText)
        recommendationText = findViewById(R.id.recommendationText)
        weatherImage = findViewById(R.id.weatherImage)
        information = findViewById(R.id.information)
        progressBar = findViewById(R.id.progressBar)
        lastUpdatedText = findViewById(R.id.lastUpdatedText)


        val scrollView = findViewById<ScrollView>(R.id.scrollView) // Инициализация ScrollView

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // Проверяем, что ScrollView в верхней части
            if (scrollY == 0) {
                swipeRefreshLayout.isEnabled = true // Включаем SwipeRefreshLayout
            } else {
                swipeRefreshLayout.isEnabled = false // Отключаем SwipeRefreshLayout
            }
        }

// Инициализация SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            if (!isInternetAvailable()) {
                // Сначала проверяем наличие интернета
                showError(getString(R.string.error_message_no_internet)) // Отображаем сообщение об отсутствии интернета
                swipeRefreshLayout.isRefreshing = false
            } else {
                // Если интернет доступен, проверяем, указан ли город
                val currentCity = cityText.text.toString().removePrefix(getString(R.string.city_prefix)).trim()
                if (!currentCity.isNotEmpty()) {
                    // Если город указан, обновляем данные о местоположении и погоде
                    getCurrentLocation()
                } else {
                    getCurrentLocation()
                }
            }
        }
        // Проверка сохраненной темы
        setThemeAccordingToPreference()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Слушатель для обновления интерфейса с учетом системных панелей
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Настройка кнопки настроек
        val settingsButton: Button = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        // Восстановление текущего города
        currentCityName = getCurrentCity()
        updateCityDisplay()

        // Проверка, есть ли сохраненный город, и загрузка погоды
        if (currentCityName.isNotEmpty()) {
            fetchWeatherData(currentCityName)
        }

        // Настройка кнопки информации
        information.setOnClickListener {
            val intent = Intent(this, InformationActivity::class.java)
            startActivity(intent)
        }

        // Попытка получить текущее местоположение
        getCurrentLocation()
    }

    private fun loadBannerAd(adSize: BannerAdSize): BannerAdView {
        return binding.banner.apply {
            setAdSize(adSize)
            setAdUnitId("R-M-13560612-1")
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    // Проверка на destroyed перед использованием
                    if (isDestroyed) {
                        bannerAd?.destroy()
                        return
                    }
                    println("YandexAds загружена")
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    println("YandexAds ошибка") // Логирование ошибки
                    Log.e("AdsError", "YandexAds ошибка: ${error.toString()}")
                }

                override fun onAdClicked() {
                    println("YandexAds реклама нажата")
                }

                override fun onLeftApplication() {
                    println("YandexAds реклама после нажатии")
                }

                override fun onReturnedToApplication() {
                    println("YandexAds возрат пользователя")
                }

                override fun onImpression(impressionData: ImpressionData?) {
                    impressionData?.let {
                        println("YandexAds регистрация показа " + it.rawData)
                    }
                }
            })
            loadAd(
                AdRequest.Builder()
                    .build()
            )
        }
    }




    // Function to show settings dialog
    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val cityEditText: AutoCompleteTextView = dialogView.findViewById(R.id.cityEditText)
        val cityListView: ListView = dialogView.findViewById(R.id.cityListView)

        // Updated list of cities
        val cities = arrayOf(
            getString(R.string.city_current_location),
            getString(R.string.city_moscow),
            getString(R.string.city_saint_petersburg),
            getString(R.string.city_kazan),
            getString(R.string.city_novosibirsk),
            getString(R.string.city_yekaterinburg),
            getString(R.string.city_nizhny_novgorod),
            getString(R.string.city_chelyabinsk),
            getString(R.string.city_samara),
            getString(R.string.city_ufa),
            getString(R.string.city_rostov_on_don),
            getString(R.string.city_voronezh),
            getString(R.string.city_krasnoyarsk),
            getString(R.string.city_tolyatti),
            getString(R.string.city_izhevsk),
            getString(R.string.city_barnaul),
            getString(R.string.city_magnitogorsk)
        )

        // Adapter for AutoCompleteTextView
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
        cityEditText.setAdapter(adapter)

        // Adapter for the city ListView
        val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cities)
        cityListView.adapter = listAdapter

        // Show the city list
        cityListView.visibility = View.VISIBLE

        // Listener for selecting from the list
        cityListView.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = cities[position]
            if (selectedCity == getString(R.string.current_location_option)) { //Определить местоположение
                // If the user selects to determine location, fetch it
                getCurrentLocation()
            } else {
                cityEditText.setText(selectedCity) // Set the selected city in AutoCompleteTextView
                cityListView.visibility = View.GONE // Hide the list when an item is selected
            }
        }

        val dialog = AlertDialog.Builder(this, R.style.TransparentDialogTheme)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save_button_text)) { _, _ ->
                val newCity = cityEditText.text.toString()
                if (newCity.isNotEmpty() && newCity != getString(R.string.current_location_option)) {
                    saveCurrentCity(newCity) // Сохраняем город
                    fetchWeatherData(newCity) // Загружаем данные о погоде
                } else {
                    showError(getString(R.string.no_city_selected))
                }
            }
            .setNegativeButton(getString(R.string.cancel_button_text)) { dialog, _ -> dialog.dismiss() }
            .create()

// Customize button text colors after the dialog is created
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.button_background))
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.button_background))
        }

        dialog.show()


        cityListView.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = cities[position]
            if (selectedCity == getString(R.string.current_location_option)) { //Определить местоположение
                getCurrentLocation()
                dialog.dismiss() // Close the dialog
            } else {
                cityEditText.setText(selectedCity)
                cityListView.visibility = View.GONE // Hide the list when an item is selected
            }
        }

        dialog.show()
    }


    private fun getCurrentLocation() {
        // Проверяем разрешение на доступ к местоположению
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Если разрешение предоставлено, получаем местоположение
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location: Location? ->
                if (location != null) {
                    // Если местоположение получено, используем его
                    fetchWeatherData("${location.latitude},${location.longitude}")
                } else {
                    // Если местоположение недоступно, используем IP для определения местоположения
                    getWeatherByIP()
                }
            }.addOnFailureListener {
                // В случае ошибки при получении местоположения, также используем IP
                getWeatherByIP()
            }
        } else {
            // Если разрешение не предоставлено, запрашиваем его
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun getWeatherByIP() {
        if (!isInternetAvailable()) {
            // Проверяем наличие интернета перед выполнением сетевого запроса
            showError(getString(R.string.error_message_no_internet)) // Отображаем сообщение об отсутствии интернета
            return
        }

        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://ipinfo.io/json") // Используем HTTPS
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonData = response.body.string()
                        jsonData.let {
                            val jsonObject = JSONObject(it)
                            val city = jsonObject.getString("city")
                            println("Определено местоположение: $city")

                            // Используем runOnUiThread для вызова fetchWeatherData(city)
                            runOnUiThread {
                                fetchWeatherData(city)
                            }
                        }
                    } else {
                        runOnUiThread {
                            showError(getString(R.string.error_message_fetch_data)) // Ошибка загрузки данных
                            println("Не удалось определить местоположение по IP.")
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showError(getString(R.string.error_message_fetch_data)) // Ошибка загрузки данных
                    println("Ошибка при получении местоположения по IP: ${e.message}")
                }
            }
        }
    }

    private fun fetchWeatherData(location: String) {
        if (!isInternetAvailable()) {
            showError(getString(R.string.error_message_no_internet)) // Отображаем сообщение об отсутствии интернета
            return
        }
        progressBar.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = true // Показать индикатор обновления

        thread {
            val client = OkHttpClient()
            val apiKey = "8781514e8a924488b99124630242610"
            val request = Request.Builder()
                .url("https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$location&days=5")
                .build()

            client.newCall(request).execute().use { response: Response ->
                if (response.isSuccessful) {
                    val jsonData = response.body.string()
                    jsonData.let {
                        val jsonObject = JSONObject(it)
                        val forecast = jsonObject.getJSONObject("forecast")
                        val forecastDays = forecast.getJSONArray("forecastday") // Извлечение массива forecastday
                        val forecastDay = forecast.getJSONArray("forecastday").getJSONObject(0)

                        val locationObject = jsonObject.getJSONObject("location")
                        val current = jsonObject.getJSONObject("current")

                        val cityName = locationObject.getString("name")
                        val tempC = current.getDouble("temp_c")
                        val condition = current.getJSONObject("condition").getString("text")
                        val precipMm = current.getDouble("precip_mm")
                        val humidity = current.getDouble("humidity")
                        val windKph = current.getDouble("wind_kph")
                        val weatherIconUrl = current.getJSONObject("condition").getString("icon")
                        val lastUpdated = current.getString("last_updated")
                        val minTempC = forecastDay.getJSONObject("day").getDouble("mintemp_c")
                        val maxTempC = forecastDay.getJSONObject("day").getDouble("maxtemp_c")
                        val feelslikeС = current.getString("feelslike_c")
                        val pressureMb = current.getDouble("pressure_mb") // Добавлено давление на будущее
                        // Создаем список для 5-дневного прогноза
                        val dailyForecasts = mutableListOf<DailyForecast>()
                        for (i in 0 until forecastDays.length()) { // Убедитесь, что цикл проходит по всем дням
                            val day = forecastDays.getJSONObject(i)
                            val date = day.getString("date")
                            val dayInfo = day.getJSONObject("day")
                            val maxTemp = dayInfo.getDouble("maxtemp_c")
                            val minTemp = dayInfo.getDouble("mintemp_c")
                            val conditionText = dayInfo.getJSONObject("condition").getString("text")
                            val conditionIcon = dayInfo.getJSONObject("condition").getString("icon")

                            dailyForecasts.add(DailyForecast(date, maxTemp, minTemp, conditionText, conditionIcon))
                        }


                        // Получаем данные на следующие 24 часа
                        val hourlyWeatherList = getHourlyWeatherForNext24Hours(forecastDays)

                        currentCityName = cityName
                        saveCurrentCity(cityName)




                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false

                            cityText.text = "${getString(R.string.city_prefix)} $cityName"
                            recommendationText.text = getRecommendation(tempC, precipMm)


                            // Устанавливаем адаптер для RecyclerView
                            val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
                            recyclerView.layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                            recyclerView.adapter = HourlyWeatherAdapter(hourlyWeatherList)

                            val recyclerView1 = findViewById<RecyclerView>(R.id.recyclerView1)
                            recyclerView1.layoutManager = LinearLayoutManager(this)
                            recyclerView1.adapter = DailyForecastAdapter(dailyForecasts)

                            if (!isDestroyed) {
                                val iconUrl = "https:${weatherIconUrl}"
                                Glide.with(this@MainActivity)
                                    .load(iconUrl)
                                    .into(weatherImage)
                            }

                            findViewById<TextView>(R.id.weatherStatus).text = getWeatherStatus(condition)
                            findViewById<TextView>(R.id.tempValue).text = "$tempC °C"
                            findViewById<TextView>(R.id.precipValue).text = "$humidity %"

                            val windMps = windKph / 3.6
                            findViewById<TextView>(R.id.windValue).text = "%.1f м/с".format(windMps)

                            val windDirection = current.getString("wind_dir")
                            setWindDirection(windDirection)


                            findViewById<TextView>(R.id.precipMmValue).text= "$precipMm мм"

                            findViewById<TextView>(R.id.mintempCValue).text= "$minTempC °C"

                            findViewById<TextView>(R.id. maxtempCValue).text= "$maxTempC °C"


                            findViewById<TextView>(R.id. avgValue).text= "$feelslikeС °C"

                            // Устанавливаем время последнего обновления
                            lastUpdatedText.text = "${getString(R.string.last_updated)}: $lastUpdated" //Последнее обновление:

                        }
                    }
                } else {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        swipeRefreshLayout.isRefreshing = false  // Скрываем индикатор обновления
                        showError(getString(R.string.error_message_fetch_data)) //Ошибка загрузки данных. Попробуйте снова.

                    }
                }
            }
        }




    }

    private fun setWindDirection(windDirection: String) {
        val windDirectionImageView = findViewById<ImageView>(R.id.iconWind)
        val windDirectionTextView = findViewById<TextView>(R.id.windDirectionText)

        // Устанавливаем изображение и текст в зависимости от направления, откуда дует ветер

        when (windDirection) {
            "N" -> {
                windDirectionImageView.setImageResource(R.drawable.s) // Стрелка указывает на север
                windDirectionTextView.text = "С" // Север
            }
            "NNE" -> {
                windDirectionImageView.setImageResource(R.drawable.ws) // Стрелка указывает на северо-северо-восток
                windDirectionTextView.text = "ССВ"
            }
            "NE" -> {
                windDirectionImageView.setImageResource(R.drawable.ws) // Стрелка указывает на северо-восток
                windDirectionTextView.text = "СВ"
            }
            "ENE" -> {
                windDirectionImageView.setImageResource(R.drawable.ws) // Стрелка указывает на восток-северо-восток
                windDirectionTextView.text = "ВСВ"
            }
            "E" -> {
                windDirectionImageView.setImageResource(R.drawable.w) // Стрелка указывает на восток
                windDirectionTextView.text = "В"
            }
            "ESE" -> {
                windDirectionImageView.setImageResource(R.drawable.nw) // Стрелка указывает на восток-юго-восток
                windDirectionTextView.text = "ВЮВ"
            }
            "SE" -> {
                windDirectionImageView.setImageResource(R.drawable.nw) // Стрелка указывает на юго-восток
                windDirectionTextView.text = "ЮВ"
            }
            "SSE" -> {
                windDirectionImageView.setImageResource(R.drawable.nw) // Стрелка указывает на юг-юго-восток
                windDirectionTextView.text = "ЮЮВ"
            }
            "S" -> {
                windDirectionImageView.setImageResource(R.drawable.n) // Стрелка указывает на юг
                windDirectionTextView.text = "Ю"
            }
            "SSW" -> {
                windDirectionImageView.setImageResource(R.drawable.sww) // Стрелка указывает на юг-юго-запад
                windDirectionTextView.text = "ЮЮЗ"
            }
            "SW" -> {
                windDirectionImageView.setImageResource(R.drawable.sww) // Стрелка указывает на юго-запад
                windDirectionTextView.text = "ЮЗ"
            }
            "WSW" -> {
                windDirectionImageView.setImageResource(R.drawable.sww) // Стрелка указывает на запад-юго-запад
                windDirectionTextView.text = "ЗЮЗ"
            }
            "W" -> {
                windDirectionImageView.setImageResource(R.drawable.e) // Стрелка указывает на запад
                windDirectionTextView.text = "З"
            }
            "WNW" -> {
                windDirectionImageView.setImageResource(R.drawable.nz) // Стрелка указывает на запад-северо-запад
                windDirectionTextView.text = "ЗСЗ"
            }
            "NW" -> {
                windDirectionImageView.setImageResource(R.drawable.nz) // Стрелка указывает на северо-запад
                windDirectionTextView.text = "СЗ"
            }
            "NNW" -> {
                windDirectionImageView.setImageResource(R.drawable.nz) // Стрелка указывает на северо-северо-запад
                windDirectionTextView.text = "ССЗ"
            }
            else -> {
                windDirectionImageView.setImageResource(R.drawable.icon_wind) // Используем изображение по умолчанию для неизвестного направления
                windDirectionTextView.text = "?"
            }
        }
    }

    fun getHourlyWeatherForNext24Hours(forecastDays: JSONArray): List<HourlyWeather> {
        val currentDateTime = LocalDateTime.now(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val hourlyWeatherList = mutableListOf<HourlyWeather>()

        // Проходим по всем доступным дням прогноза
        for (dayIndex in 0 until forecastDays.length()) {
            val dayData = forecastDays.getJSONObject(dayIndex)
            val hourlyForecast = dayData.getJSONArray("hour")

            for (i in 0 until hourlyForecast.length()) {
                val hourData = hourlyForecast.getJSONObject(i)
                val time = hourData.getString("time")
                val hourTempC = hourData.getDouble("temp_c")
                val hourIconUrl = hourData.getJSONObject("condition").getString("icon")

                // Преобразуем строку времени в LocalDateTime
                val forecastTime = LocalDateTime.parse(time, formatter)

                // Добавляем данные, если они находятся в пределах следующих 24 часов
                if (forecastTime.isAfter(currentDateTime) || forecastTime.isEqual(currentDateTime)) {
                    hourlyWeatherList.add(HourlyWeather(time, hourTempC, hourIconUrl))
                }

                // Останавливаем, если собрали 24 часа
                if (hourlyWeatherList.size == 24) {
                    return hourlyWeatherList
                }
            }
        }

        return hourlyWeatherList
    }

    private fun getCurrentSeason(): String {
        val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1 // Calendar months are zero-based
        return when (month) {
            12, 1, 2 -> "winter"
            3, 4, 5 -> "spring"
            6, 7, 8 -> "summer"
            9, 10, 11 -> "autumn"
            else -> "unknown"
        }
    }
    // Добавьте функцию для проверки наличия подключения к интернету
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getWeatherStatus(condition: String): String {
        val conditionLower = condition.lowercase(Locale.ROOT)
        return when (conditionLower) {
            "clear" -> getString(R.string.weather_clear)
            "partly cloudy" -> getString(R.string.weather_partly_cloudy)
            "cloudy" -> getString(R.string.weather_cloudy)
            "overcast" -> getString(R.string.weather_overcast)
            "mist" -> getString(R.string.weather_mist)
            "fog" -> getString(R.string.weather_fog)
            "sand" -> getString(R.string.weather_sand)
            "dust" -> getString(R.string.weather_dust)
            "ash" -> getString(R.string.weather_ash)
            "snow" -> getString(R.string.weather_snow)
            "snow showers" -> getString(R.string.weather_snow_showers)
            "hail" -> getString(R.string.weather_hail)
            "sleet" -> getString(R.string.weather_sleet)
            "rain" -> getString(R.string.weather_rain)
            "rain showers" -> getString(R.string.weather_rain_showers)
            "thunderstorm" -> getString(R.string.weather_thunderstorm)
            "tornado" -> getString(R.string.weather_tornado)
            "hurricane" -> getString(R.string.weather_hurricane)
            "tropical storm" -> getString(R.string.weather_tropical_storm)
            "cold" -> getString(R.string.weather_cold)
            "hot" -> getString(R.string.weather_hot)
            "very hot" -> getString(R.string.weather_very_hot)
            "very cold" -> getString(R.string.weather_very_cold)
            "blizzard" -> getString(R.string.weather_blizzard)
            "extreme cold" -> getString(R.string.weather_extreme_cold)
            "moderate rain" -> getString(R.string.weather_moderate_rain)
            "light snow" -> getString(R.string.weather_light_snow)
            "moderate snow" -> getString(R.string.weather_moderate_snow)
            "heavy snow" -> getString(R.string.weather_heavy_snow)
            "patchy snow possible" -> getString(R.string.weather_patchy_snow_poss)
            "light snow showers" -> getString(R.string.weather_light_snow_showers)
            "sunny"-> getString(R.string.weather_sunny)
            "freezing fog"-> getString(R.string.weather_freezing_fog)
            "blowing snow" -> getString(R.string.weather_blowing_snow)
            else -> condition // Return the original status if unknown
        }
    }





    private fun getRecommendation(temp: Double, precip: Double): String {
        val currentSeason = getCurrentSeason()

        return when (currentSeason) {
            "winter" -> {
                // If it's winter, recommend winter tyres or studded tyres according to regulations
                getString(R.string.recommend_winter_tyres_studded)
            }
            "summer" -> {
                // If it's summer, recommend summer tyres according to regulations
                getString(R.string.recommend_summer_tyres)
            }
            "spring", "autumn" -> {
                // For spring or autumn, check temperature and precipitation
                when {
                    temp < 0 -> getString(R.string.recommend_winter_tyres) // Below 0°C, recommend winter tyres
                    temp in 0.0..10.0 -> {
                        if (precip > 5) {
                            getString(R.string.recommend_winter_tyres_rainy) // Cold and rainy, recommend winter tyres
                        } else {
                            getString(R.string.recommend_all_seasons) // Mild temperatures, recommend all-season tyres
                        }
                    }
                    temp > 10 -> {
                        if (precip < 5) {
                            getString(R.string.recommend_summer_tyres) // Warm and dry, recommend summer tyres
                        } else {
                            getString(R.string.consider_condition) // Warm and rainy, consider current conditions
                        }
                    }
                    else -> getString(R.string.weather_unstable) // Fallback for undefined conditions
                }
            }
            else -> getString(R.string.weather_unstable) // Unknown season fallback
        }
    }




    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.error_title)) //ошибка на разных языках
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено, теперь мы можем получить местоположение
                getCurrentLocation()
            } else {
                showError(getString(R.string.error_message_location_permission)) //Разрешение на доступ к местоположению не предоставлено.

            }
        }
    }
    private fun setThemeAccordingToPreference() {
        when {
            isDarkModeEnabled() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            isDarkModeNotSet() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Обновляем текст города после смены темы
        updateCityDisplay()

        // Загрузка данных о погоде для текущего города после смены темы
        if (currentCityName.isNotEmpty()) {
            fetchWeatherData(currentCityName)
        }
    }



    private fun updateCityDisplay() {
        cityText.text = "${getString(R.string.city_prefix)} ${currentCityName}"
    }

    private fun saveCurrentCity(cityName: String) {
        getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("current_city", cityName)
            .apply()
    }

    private fun getCurrentCity(): String {
        return getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            .getString("current_city", "") ?: ""
    }


    private fun isDarkModeEnabled(): Boolean {
        return getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            .getBoolean("dark_mode", false)
    }

    private fun isDarkModeNotSet(): Boolean {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return !prefs.contains("dark_mode")
    }
    private fun getSavedLanguage(): String {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("app_language", null)

        // Return saved language if exists, or device's default language
        return savedLanguage ?: Locale.getDefault().language
    }

    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)

        // Примените новую конфигурацию
        val context = createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Сохраните язык в shared preferences
        val editor = getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()
        editor.putString("app_language", lang)
        editor.apply()

        updateTextViews() // Обновите тексты интерфейса сразу после изменения локали
    }

    private fun updateTextViews() {
        if (this::cityText.isInitialized) {
            cityText.text = "${getString(R.string.city_prefix)} $currentCityName"






        }
    }



}
data class HourlyWeather(
    val time: String,
    val temperature: Double,
    val iconUrl: String
)
data class DailyForecast(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val conditionText: String,
    val conditionIcon: String
)