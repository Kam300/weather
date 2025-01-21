package com.example.weathertyre.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.example.weathertyre.DatabaseHelper
import com.example.weathertyre.MainActivity
import com.example.weathertyre.databinding.ActivityLoginBinding
import com.example.weathertyre.R
import com.example.weathertyre.RegisterActivity
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        setLocale(getSavedLanguage()) // Set the language before super
        super.onCreate(savedInstanceState)


        dbHelper = DatabaseHelper()
        setThemeAccordingToPreference()
        // Проверяем, залогинен ли пользователь
        if (dbHelper.isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val register = binding.register
        val usernameInput = binding.usernameInput // ID TextInputEditText в layout
        val passwordInput = binding.passwordInput // ID TextInputEditText в layout
        val login = binding.login
        val loading = binding.loading

        if (register != null) {
            register.setOnClickListener {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            }
        }

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                if (usernameInput != null) {
                    usernameInput.error = getString(loginState.usernameError)
                }
            } else {
                if (usernameInput != null) {
                    usernameInput.error = null
                }
            }
            if (loginState.passwordError != null) {
                if (passwordInput != null) {
                    passwordInput.error = getString(loginState.passwordError)
                }
            } else {
                if (passwordInput != null) {
                    passwordInput.error = null
                }
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                setResult(Activity.RESULT_OK)
                finish()
            }
        })

        if (usernameInput != null) {
            usernameInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (passwordInput != null) {
                        loginViewModel.loginDataChanged(
                            usernameInput.text.toString(),
                            passwordInput.text.toString()
                        )
                    }
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            })
        }

        if (passwordInput != null) {
            passwordInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (usernameInput != null) {
                        loginViewModel.loginDataChanged(
                            usernameInput.text.toString(),
                            passwordInput.text.toString()
                        )
                    }
                }

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            })
        }

        if (passwordInput != null) {
            passwordInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (usernameInput != null) {
                        loginViewModel.login(
                            usernameInput.text.toString(),
                            passwordInput.text.toString()
                        )
                    }
                    true
                } else {
                    false
                }
            }
        }

        login.setOnClickListener {
            loading.visibility = View.VISIBLE
            if (passwordInput != null) {
                if (usernameInput != null) {
                    loginViewModel.login(
                        usernameInput.text.toString(),
                        passwordInput.text.toString()
                    )
                }
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, getString(errorString), Toast.LENGTH_SHORT).show()
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


    }
}
