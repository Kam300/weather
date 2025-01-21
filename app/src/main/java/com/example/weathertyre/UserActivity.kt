package com.example.weathertyre

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.lifecycleScope
import com.example.weathertyre.databinding.ActivityUserBinding
import com.example.weathertyre.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.jan.supabase.gotrue.gotrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class UserProfile(
    @SerialName("id")
    val id: Int,
    @SerialName("user_id")
    val userId: String,
    @SerialName("email")
    val email: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)class UserActivity : AppCompatActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var binding: ActivityUserBinding
    private var loadingDialog: AlertDialog? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityUserBinding.inflate(layoutInflater)
            setContentView(binding.root)

            databaseHelper = DatabaseHelper()

            if (!databaseHelper.isUserLoggedIn()) {
                navigateToLogin()
                return
            }

            setupViews()
            loadUserProfile()
        } catch (e: Exception) {
            Log.e("UserActivity", "Error in onCreate: ${e.message}")
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showLoadingDialog(message: String = "Загрузка...") {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        val messageText = dialogView.findViewById<TextView>(R.id.loadingMessage)
        messageText.text = message

        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun performDeleteAccount() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Подтверждение удаления")
            .setMessage("Это действие нельзя отменить. Все ваши данные будут удалены. Вы уверены?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    try {
                        showLoadingDialog("Удаление аккаунта...")

                        withContext(Dispatchers.IO) {
                            val currentUser = databaseHelper.supabase.gotrue.currentSessionOrNull()?.user
                            val userId = currentUser?.id ?: throw Exception("Пользователь не авторизован")

                            // Удаляем аккаунт
                            databaseHelper.deleteUserData(userId)

                            // Очищаем локальные данные
                            clearLocalData()

                            withContext(Dispatchers.Main) {
                                hideLoadingDialog()
                                Toast.makeText(
                                    this@UserActivity,
                                    "Аккаунт успешно удален",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigateToLogin()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("UserActivity", "Error deleting account: ${e.message}")
                        withContext(Dispatchers.Main) {
                            hideLoadingDialog()
                            Toast.makeText(
                                this@UserActivity,
                                "Ошибка при удалении аккаунта: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                showLoadingDialog("Выход из системы...")
                withContext(Dispatchers.IO) {
                    databaseHelper.supabase.gotrue.logout()
                    clearLocalData()
                }
                hideLoadingDialog()
                navigateToLogin()
            } catch (e: Exception) {
                Log.e("UserActivity", "Error during logout: ${e.message}")
                hideLoadingDialog()
                Toast.makeText(
                    this@UserActivity,
                    "Ошибка при выходе: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun clearLocalData() {
        getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoadingDialog()
    }

    private fun setThemeAccordingToPreference() {
        when {
            isDarkModeEnabled() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            isDarkModeNotSet() -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun isDarkModeEnabled(): Boolean {
        return getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            .getBoolean("dark_mode", false)
    }

    private fun isDarkModeNotSet(): Boolean {
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return !prefs.contains("dark_mode")
    }
    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupViews() {
        binding.apply {
            backButton.setOnClickListener {
                finish()
            }

            logoutButton.setOnClickListener {
                performLogout()
            }

            deleteAccountButton.setOnClickListener {
                performDeleteAccount()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val session = databaseHelper.supabase.gotrue.currentSessionOrNull()
                    ?: throw Exception("Сессия не найдена")

                val userId = session.user?.id
                    ?: throw Exception("ID пользователя не найден")

                withContext(Dispatchers.IO) {
                    val userProfile = databaseHelper.getUserProfile(userId)

                    withContext(Dispatchers.Main) {
                        try {
                            binding.apply {
                                emailText.text = userProfile.email

                                // Форматирование даты
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                                val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

                                val date = inputFormat.parse(userProfile.createdAt)
                                registrationDateText.text = outputFormat.format(date)
                            }
                        } catch (e: Exception) {
                            Log.e("UserActivity", "Error formatting date: ${e.message}")
                            binding.registrationDateText.text = userProfile.createdAt
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UserActivity", "Error loading profile: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@UserActivity,
                        "Ошибка загрузки профиля: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}