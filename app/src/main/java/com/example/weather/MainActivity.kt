package com.example.weather

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.weather.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

private lateinit var fusedLocationClient: FusedLocationProviderClient
private val currentVersion = "1.0.8"

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

            // Возвращаем "липкий" размер для отображения рекламы внизу экрана
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

        scrollView.setOnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
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
            val currentCity = cityText.text.toString().removePrefix(getString(R.string.city_prefix)).trim()
            if (currentCity.isNotEmpty()) {
                fetchWeatherData(currentCity)
            } else {
                showError(getString(R.string.no_city_selected)) // Пожалуйста, выберите город.
                swipeRefreshLayout.isRefreshing = false
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

        checkForUpdates()

        // Попытка получить текущее местоположение
        getCurrentLocation()
    }

    private fun loadBannerAd(adSize: BannerAdSize): BannerAdView {
        return binding.banner.apply {
            setAdSize(adSize)
            setAdUnitId("demo-banner-yandex")
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    // Проверка на destroyed перед использованием
                    if (isDestroyed) {
                        bannerAd?.destroy()
                        return
                    }
                    println("YandexAds загружена")
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    println("YandexAds ошибка") // Логирование ошибки
                    Log.e("AdsError", "YandexAds ошибка: ${adRequestError.toString()}")
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


    private fun checkForUpdates() {
        val url = "https://raw.githubusercontent.com/Kam300/URL/refs/heads/main/versionweapson.json"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { jsonResponse ->
                        val updateInfo = Gson().fromJson(jsonResponse, UpdateInfo::class.java)

                        runOnUiThread {
                            if (updateInfo.version != currentVersion) {
                                showUpdateDialog(updateInfo.url)
                            } else {
                                Toast.makeText(this@MainActivity, getString(R.string.no_update_available), Toast.LENGTH_SHORT).show()  //Вы используете последнюю версию.
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, getString(R.string.error_message_fetch_data), Toast.LENGTH_SHORT).show() //Не удалось проверить наличие обновлений
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, getString(R.string.error_message_location_permission), Toast.LENGTH_SHORT).show() //Ошибка сети

                }
            }
        })
    }

    private fun showUpdateDialog(url: String) {
        // Inflate the custom layout for the dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_update, null)

        // Find views in the inflated layout
        val dialogTitle: TextView = dialogView.findViewById(R.id.dialogTitle)
        val dialogMessage: TextView = dialogView.findViewById(R.id.dialogMessage)
        val buttonDownload: Button = dialogView.findViewById(R.id.buttonDownload)
        val buttonCancel: Button = dialogView.findViewById(R.id.buttonCancel)
        val downloadProgressBar: ProgressBar = dialogView.findViewById(R.id.downloadProgressBar)

        // Set the title and message (you can customize them further if needed)
        dialogTitle.text = getString(R.string.update_available) //"Доступно обновление"
        dialogMessage.text = getString(R.string.update_dialog_message) //"Доступна новая версия. Хотели бы вы ее скачать?

        // Build the dialog
        val dialog = AlertDialog.Builder(this, R.style.TransparentDialogTheme)
            .setView(dialogView)
            .setCancelable(false) // Делаем диалог неотменяемым
            .create()

        // Set click listener for the download button
        buttonDownload.setOnClickListener {
            downloadApk(url, downloadProgressBar) // Передаем ProgressBar в метод загрузки
            dialog.dismiss() // Дисмисс диалога сразу
            dialog.show() // Показываем диалог
        }

        // Set click listener for the cancel button
        buttonCancel.setOnClickListener {
            dialog.dismiss() // Just dismiss the dialog
        }

        dialog.show() // Show the dialog
    }

    private fun downloadApk(apkUrl: String, progressBar: ProgressBar) {
        val destinationDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(destinationDir, "yourapp-${currentVersion}.apk")

        // Create the download request
        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle(getString(R.string.notifications))
            setDescription(getString(R.string.downloading_update))
            setDestinationUri(Uri.fromFile(apkFile)) // Note: this line works for download manager
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            allowScanningByMediaScanner() // Allow file scanning
        }

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Start tracking progress in a separate thread
        Thread {
            var downloading = true
            runOnUiThread {
                progressBar.visibility = View.VISIBLE // Show ProgressBar at the start
            }

            while (downloading) {
                val query = DownloadManager.Query()
                query.setFilterById(downloadId)

                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    if (statusIndex != -1 && bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                        val status = cursor.getInt(statusIndex)
                        val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
                        val bytesTotal = cursor.getInt(bytesTotalIndex)

                        when (status) {
                            DownloadManager.STATUS_RUNNING -> {
                                val progress = (bytesDownloaded * 100L / bytesTotal).toInt()
                                runOnUiThread {
                                    progressBar.progress = progress
                                }
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this, getString(R.string.complete_download), Toast.LENGTH_SHORT).show()

                                    // Install APK

                                    val uri = FileProvider.getUriForFile(
                                        this,
                                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                                        apkFile
                                    )



                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read URI
                                    startActivity(intent)
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this, getString(R.string.error_download), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    cursor.close()
                }
            }
        }.start()

        Toast.makeText(this, getString(R.string.now_downloading_update), Toast.LENGTH_SHORT).show()
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
                location?.let {
                    fetchWeatherData("${it.latitude},${it.longitude}")
                } ?: run {
                    showError("Не удалось получить ваше текущее местоположение. Пожалуйста, выберите город.")
                }
            }
        } else {
            // Если разрешение не предоставлено, запрашиваем его
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun fetchWeatherData(location: String) {
        progressBar.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = true  // Показать индикатор обновления
        thread {
            val client = OkHttpClient()
            val apiKey = "8781514e8a924488b99124630242610"
            val request = Request.Builder()
                .url("https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$location&days=1")
                .build()


            client.newCall(request).execute().use { response: Response ->
                if (response.isSuccessful) {
                    val jsonData = response.body?.string()
                    jsonData?.let {


                        val jsonObject = JSONObject(it)
                        val forecast = jsonObject.getJSONObject("forecast")
                        val forecastDay = forecast.getJSONArray("forecastday").getJSONObject(0) // Берем первый день

                        val locationObject = jsonObject.getJSONObject("location")
                        val current = jsonObject.getJSONObject("current")

                        val cityName = locationObject.getString("name")
                        val tempC = current.getDouble("temp_c")
                        val condition = current.getJSONObject("condition").getString("text")
                        val precipMm = current.getDouble("precip_mm")
                        val humidity = current.getDouble("humidity") // Получаем влажность
                        val windKph = current.getDouble("wind_kph")
                        val weatherIconUrl = current.getJSONObject("condition").getString("icon")
                        val lastUpdated = current.getString("last_updated")
                        // Check if min and max temperature values exist
                        val minTempC = forecastDay.getJSONObject("day").getDouble("mintemp_c")
                        val maxTempC = forecastDay.getJSONObject("day").getDouble("maxtemp_c")

                        val feelslikeС = current.getString("feelslike_c")


                        currentCityName = cityName
                        saveCurrentCity(cityName)
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false  // Скрываем индикатор обновления
                            cityText.text = "${getString(R.string.city_prefix)} $cityName" //Город

                            recommendationText.text = getRecommendation(tempC, condition, precipMm, windKph)

                            // Загрузите изображение погоды
                            val iconUrl = "https:${weatherIconUrl}"
                            Glide.with(this@MainActivity)
                                .load(iconUrl)
                                .into(weatherImage)

                            // Установите текст статуса погоды
                            findViewById<TextView>(R.id.weatherStatus).text = getWeatherStatus(condition)




                            // Устанавливаем значения иконок и текстов
                            findViewById<TextView>(R.id.tempValue).text = "$tempC °C"
                            findViewById<TextView>(R.id.precipValue).text = "$humidity %"
                            findViewById<TextView>(R.id.windValue).text = "$windKph км/ч"

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


    private fun getWeatherStatus(condition: String): String {
        val conditionLower = condition.toLowerCase()
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
            "Freezing fog<"-> getString(R.string.weather_freezing_fog)
            else -> condition // Return the original status if unknown
        }
    }





    private fun getRecommendation(temp: Double, condition: String, precip: Double, wind: Double): String {
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
        resources.updateConfiguration(config, resources.displayMetrics)

        // Save language to shared preferences
        val editor = getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit()
        editor.putString("app_language", lang)
        editor.apply()

        updateTextViews()  // Update UI texts immediately after changing the locale
    }
    private fun updateTextViews() {
        if (this::cityText.isInitialized) {
            cityText.text = "${getString(R.string.city_prefix)} $currentCityName"






        }
    }



}
