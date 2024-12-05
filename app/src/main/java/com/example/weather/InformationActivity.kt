package com.example.weather

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
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
import java.io.File
import java.io.IOException
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import java.util.Locale


data class UpdateInfo(val version: String, val url: String)

class InformationActivity : AppCompatActivity() {
    private val currentVersion = "1.0.8"
    private val REQUEST_CODE_STORAGE = 1001 // Код запроса для разрешений



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Проверка сохраненной темы
        setThemeAccordingToPreference()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.information)

        // Запрос разрешения на чтение/запись во внешнем хранилище
        requestStoragePermissions()

        val linkButton: Button = findViewById(R.id.linkButton)
        val linkButton2: Button = findViewById(R.id.linkButton22)
        val checkUpdateButton: Button = findViewById(R.id.checkUpdateButton)
        val beforeButton: Button = findViewById(R.id.beforeButton)
        val themeSettingsButton: Button = findViewById(R.id.themeSettingsButton )
        val languageSettingsButton: Button = findViewById(R.id.languageSettingsButton)
        languageSettingsButton.setOnClickListener {
            showLanguageSelectionDialog()
        }
        themeSettingsButton.setOnClickListener {
            val bottomSheet = ThemeBottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }




        linkButton2.setOnClickListener {
            val url = "http://comgamedev.gilect.net/?i=1"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

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
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        val isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected
        Log.d("NetworkCheck", "Network available: $isConnected")
        return isConnected
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
                    response.body?.string()?.let { jsonResponse ->
                        Log.d("UpdateCheck", "Response body: $jsonResponse")
                        val updateInfo = Gson().fromJson(jsonResponse, UpdateInfo::class.java)
                        Log.d("UpdateCheck", "Update Info: $updateInfo")

                        runOnUiThread {
                            if (updateInfo.version != currentVersion) {
                                showUpdateDialog(updateInfo.url)
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
        dialogTitle.text = getString(R.string.update_available)
        dialogMessage.text = getString(R.string.update_dialog_message)

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
