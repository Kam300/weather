package com.example.weathertyre

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonObject

class DatabaseHelper {
    val supabase = SupabaseClient.client
    private val maxRetries = 3
    private val initialRetryDelay = 2.seconds

    private suspend fun <T> withRetry(
        maxAttempts: Int = maxRetries,
        block: suspend () -> T
    ): T {
        var currentDelay = initialRetryDelay
        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: RestException) {
                if (e.message?.contains("Too Many Requests") == true) {
                    if (attempt == maxAttempts - 1) throw e
                    println("Rate limit exceeded, waiting ${currentDelay.inWholeSeconds} seconds...")
                    delay(currentDelay)
                    currentDelay *= 2
                } else {
                    throw e
                }
            }
        }
        throw Exception("Превышено максимальное количество попыток")
    }

    suspend fun registerUser(email: String, password: String) {
        try {
            withContext(Dispatchers.IO) {
                println("Attempting to register user with email: $email")

                // Регистрация пользователя в Supabase
                withRetry {
                    supabase.gotrue.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        data = buildJsonObject {
                            put("email_verified", true)
                        }
                    }
                }

                // Выполняем вход для получения сессии
                withRetry {
                    supabase.gotrue.loginWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }

                // Получаем текущую сессию
                val session = supabase.gotrue.currentSessionOrNull()
                val userId = session?.user?.id
                    ?: throw Exception("Не удалось получить ID пользователя")

                // Сохраняем дополнительные данные пользователя
                insertUserData(userId, email)

                println("User registered successfully with email: $email and ID: $userId")
            }
        } catch (e: RestException) {
            when {
                e.message?.contains("User already registered") == true -> {
                    println("User already registered: $email")
                    throw Exception("Этот email уже зарегистрирован")
                }

                e.message?.contains("Too Many Requests") == true -> {
                    throw Exception("Сервис временно недоступен. Пожалуйста, попробуйте позже")
                }

                else -> {
                    println("Error during registration: ${e.message}")
                    throw Exception("Ошибка при регистрации: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("General error during registration: ${e.message}")
            throw Exception("Ошибка при регистрации: ${e.message}")
        }
    }

    suspend fun insertUserData(userId: String, email: String) {
        if (userId.isBlank()) {
            throw Exception("ID пользователя не может быть пустым")
        }

        try {
            withContext(Dispatchers.IO) {
                withRetry {
                    val userData = buildJsonObject {
                        put("user_id", userId)
                        put("email", email)
                        // Убираем created_at, так как этого поля нет в таблице
                    }

                    supabase.postgrest
                        .from("users")
                        .insert(userData)
                }
            }
        } catch (e: Exception) {
            println("Error inserting user data: ${e.message}")
            println("URL: ${e.message}")
            println("Headers: ${e.message}")
            println("Http Method: POST")
            throw Exception("Ошибка при сохранении данных пользователя: ${e.message}")
        }
    }

    suspend fun loginUser(email: String, password: String) {
        try {
            withContext(Dispatchers.IO) {
                withRetry {
                    supabase.gotrue.loginWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
            }
        } catch (e: RestException) {
            when {
                e.message?.contains("Invalid login credentials") == true -> {
                    throw Exception("Неверный email или пароль")
                }

                e.message?.contains("Too Many Requests") == true -> {
                    throw Exception("Слишком много попыток. Попробуйте позже")
                }

                else -> {
                    throw Exception("Ошибка при входе: ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Ошибка при входе: ${e.message}")
        }
    }

    fun isUserLoggedIn(): Boolean {
        return supabase.gotrue.currentSessionOrNull() != null
    }

    suspend fun getCurrentUserEmail(): String? {
        return supabase.gotrue.currentSessionOrNull()?.user?.email
    }

    // Добавьте константу для JSON конфигурации
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun saveTemperatureUnit(userId: String, unit: String) {
        try {
            withContext(Dispatchers.IO) {
                withRetry {
                    val settings = buildJsonObject {
                        put("user_id", userId)
                        put("temperature_unit", unit)
                    }

                    // Проверяем существование записи
                    val existingSettings = supabase.postgrest
                        .from("UserSettings")
                        .select() {
                            eq("user_id", userId)
                        }
                        .decodeList<UserSettings>()
                        .firstOrNull()

                    if (existingSettings != null) {
                        // Обновляем существующую запись
                        supabase.postgrest
                            .from("UserSettings")
                            .update(settings) {
                                eq("user_id", userId)
                            }
                    } else {
                        // Создаем новую запись
                        supabase.postgrest
                            .from("UserSettings")
                            .insert(settings)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error saving temperature unit: ${e.message}")
            throw Exception("Ошибка при сохранении единицы измерения температуры: ${e.message}")
        }
    }

    suspend fun getTemperatureUnit(userId: String): String {
        return try {
            withContext(Dispatchers.IO) {
                withRetry {
                    val response = supabase.postgrest
                        .from("UserSettings")
                        .select() {
                            eq("user_id", userId)
                        }
                        .decodeList<UserSettings>()
                        .firstOrNull()

                    response?.temperatureUnit ?: "C"
                }
            }
        } catch (e: Exception) {
            println("Error getting temperature unit: ${e.message}")
            "C" // Возвращаем Цельсий по умолчанию в случае ошибки
        }
    }

    @Serializable
    data class UserSettings(
        @SerialName("id")
        val id: Long? = null,
        @SerialName("user_id")
        val userId: String,
        @SerialName("temperature_unit")
        val temperatureUnit: String,
        @SerialName("created_at")
        val createdAt: String? = null,
        @SerialName("updated_at")
        val updatedAt: String? = null
    )
}