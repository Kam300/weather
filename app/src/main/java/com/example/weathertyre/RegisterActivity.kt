package com.example.weathertyre

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private var verificationCode: String = ""
    private var isCodeSent = false
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var verifyCodeEditText: EditText
    private lateinit var backButton: ImageButton
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        dbHelper = DatabaseHelper()
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirm_password)
        registerButton = findViewById(R.id.register_button)
        verifyCodeEditText = findViewById(R.id.verification_code)
        backButton = findViewById(R.id.back_button)
        verifyCodeEditText.visibility = View.GONE
    }

    private fun setupListeners() {
        backButton.setOnClickListener { onBackPressed() }
        registerButton.setOnClickListener { handleRegistration() }
    }

    private fun handleRegistration() {
        if (!isCodeSent) {
            handleInitialRegistration()
        } else {
            verifyCode()
        }
    }

    private fun handleInitialRegistration() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        when {
            email.isEmpty() -> {
                showToast("Введите email")
                return
            }
            !isValidEmail(email) -> {
                showToast("Введите корректный email")
                return
            }
            password.isEmpty() -> {
                showToast("Введите пароль")
                return
            }
            password.length < 6 -> {
                showToast("Пароль должен содержать минимум 6 символов")
                return
            }
            password != confirmPassword -> {
                showToast("Пароли не совпадают")
                return
            }
        }

        sendVerificationCode(email)
    }

    private fun sendVerificationCode(email: String) {
        verificationCode = generateVerificationCode()
        registerButton.isEnabled = false
        showToast("Отправка кода...")

        Thread {
            try {
                EmailSender().sendEmail(email, verificationCode)
                runOnUiThread {
                    registerButton.isEnabled = true
                    showToast("Код отправлен на вашу почту")
                    verifyCodeEditText.visibility = View.VISIBLE
                    registerButton.text = "Подтвердить код"
                    isCodeSent = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    registerButton.isEnabled = true
                    showToast("Ошибка отправки: ${e.message}")
                }
            }
        }.start()
    }

    private fun verifyCode() {
        val enteredCode = verifyCodeEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        when {
            enteredCode.isEmpty() -> {
                showToast("Введите код подтверждения")
                return
            }
            enteredCode == verificationCode -> {
                // Регистрация пользователя в базе данных
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        dbHelper.registerUser(email, password)
                        withContext(Dispatchers.Main) {
                            showToast("Регистрация успешна")
                            proceedToMainActivity()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showToast(e.message ?: "Ошибка регистрации")
                            registerButton.isEnabled = true
                        }
                    }
                }
            }
            else -> {
                showToast("Неверный код подтверждения")
            }
        }
    }

    private fun proceedToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }






    private fun generateVerificationCode(): String {
        return (100000..999999).random().toString()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}