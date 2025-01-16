package com.example.weathertyre

import android.Manifest
import android.annotation.SuppressLint

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

import java.io.IOException
import android.provider.Settings
import android.util.Log

import androidx.appcompat.app.AppCompatDelegate

import java.util.Locale



data class UpdateInfo(val version: String, val url: String)

class InformationActivity : AppCompatActivity() {
    private val currentVersion = "1.1.0"
    private val REQUEST_CODE_STORAGE = 1001 // Код запроса для разрешений
    @SuppressLint("MissingInflatedId")


    override fun onCreate(savedInstanceState: Bundle?) {
        // Проверка сохраненной темы
        setThemeAccordingToPreference() // Устанавливаем тему перед вызовом super.onCreate()
        val savedLanguage = getSavedLanguage()
        setLocale(savedLanguage) // Устанавливаем язык при запуске Activity
        super.onCreate(savedInstanceState)


        setContentView(R.layout.information)

//        // Запрос разрешения на чтение/запись во внешнем хранилище
//        requestStoragePermissions()

        val linkButton: Button = findViewById(R.id.linkButton)
//        val linkButton2: Button = findViewById(R.id.linkButton22)
        val checkUpdateButton: Button = findViewById(R.id.checkUpdateButton)
        val beforeButton: Button = findViewById(R.id.beforeButton)
        val themeSettingsButton: Button = findViewById(R.id.themeSettingsButton )
        val languageSettingsButton: Button = findViewById(R.id.languageSettingsButton)
        val politicButton: Button = findViewById(R.id.politicButton)
        languageSettingsButton.setOnClickListener {
            showLanguageSelectionDialog()
        }
        themeSettingsButton.setOnClickListener {
            val bottomSheet = ThemeBottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }



//
//        linkButton2.setOnClickListener {
//            val url = "http://comgamedev.gilect.net/?i=1"
//            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//            startActivity(intent)
//        }

        linkButton.setOnClickListener {
            val url2 = "https://t.me/ComGameDev"
            val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(url2))
            startActivity(intent1)
        }

        checkUpdateButton.setOnClickListener {
            checkForUpdates()
        }
        beforeButton.setOnClickListener {
            finish()
        }
        politicButton.setOnClickListener {
            val url2 = "https://doc-hosting.flycricket.io/politika-konfidentsialnosti-weathertyre/bfd977ea-972e-47a4-8617-e3bf18b851c8/privacy"
            val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(url2))
            startActivity(intent1)
        }
    }


    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("English", "Русский") // Add more languages as needed
        val currentLanguageIndex = getCurrentLanguageIndex()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
            .setSingleChoiceItems(languages, currentLanguageIndex) { dialog, which ->
                val langCode = if (which == 0) "en" else "ru" // Modify if you add more languages

                // Show confirmation dialog for restarting the app
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.restart_required))
                    .setMessage(getString(R.string.restart_app_message))
                    .setPositiveButton(getString(R.string.restart)) { _, _ ->
                        setLocale(langCode) // Set the selected language
                        restartApp() // Restart the application instead of just the activity
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()

                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }


    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Call finish to close the current activity
    }


    private fun getCurrentLanguageIndex(): Int {
        val savedLanguage = getSavedLanguage()
        return if (savedLanguage == "en") 0 else 1 // Adjust indices based on your language options
    }

    private fun getSavedLanguage(): String {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("app_language", null)

        // If no language is saved, return the device's default language
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
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestManageStoragePermission()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), REQUEST_CODE_STORAGE)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            val isConnected = capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )

            Log.d("NetworkCheck", "Network available: $isConnected")
            return isConnected
        } else {
            // Для старых версий Android (ниже Android 6.0)
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            val isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected
            Log.d("NetworkCheck", "Network available: $isConnected")
            return isConnected
        }
    }


    private fun checkForUpdates() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://raw.githubusercontent.com/Kam300/URL/refs/heads/main/versionweapson.json"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: Response) {
                Log.d("UpdateCheck", "Response code: ${response.code}")
                if (response.isSuccessful) {
                    response.body.string().let { jsonResponse ->
                        Log.d("UpdateCheck", "Response body: $jsonResponse")
                        val updateInfo = Gson().fromJson(jsonResponse, UpdateInfo::class.java)
                        Log.d("UpdateCheck", "Update Info: $updateInfo")

                        runOnUiThread {
                            if (updateInfo.version != currentVersion) {
                                Toast.makeText(this@InformationActivity, getString(R.string.update_available), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@InformationActivity, getString(R.string.no_update_available), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.e("UpdateCheck", "Error: ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@InformationActivity, getString(R.string.error_message_fetch_data), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("UpdateCheck", "Network failure: $e")
                runOnUiThread {
                    Toast.makeText(this@InformationActivity, getString(R.string.error_message_location_permission), Toast.LENGTH_SHORT).show()
                }
            }
        })


}


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, getString(R.string.access_ermission), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.denied_ermission), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission is granted
            } else {
                // Request permission
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.setData(Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
    }

    private fun setThemeAccordingToPreference() {
        when {
            isDarkModeEnabled() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            isDarkModeNotSet() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        setContentView(R.layout.activity_main) // Re-inflate the layout
    }


    private fun isDarkModeEnabled(): Boolean {
        return getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            .getBoolean("dark_mode", false)
    }

    private fun isDarkModeNotSet(): Boolean {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return !prefs.contains("dark_mode")
    }


}
