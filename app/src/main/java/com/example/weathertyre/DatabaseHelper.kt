package com.example.weathertyre

import android.util.Log
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
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


    // Обновляем функцию withRetry для обработки ограничений безопасности
    private suspend fun <T> withRetry(
        maxAttempts: Int = maxRetries,
        block: suspend () -> T
    ): T {
        var currentDelay = initialRetryDelay
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: RestException) {
                lastException = e
                when {
                    e.message?.contains("security purposes") == true -> {
                        if (attempt == maxAttempts - 1) throw e
                        println("Security delay required, waiting 60 seconds...")
                        delay(60000) // 60 секунд
                    }
                    e.message?.contains("Too Many Requests") == true -> {
                        if (attempt == maxAttempts - 1) throw e
                        println("Rate limit exceeded, waiting ${currentDelay.inWholeSeconds} seconds...")
                        delay(currentDelay)
                        currentDelay *= 2
                    }
                    else -> throw e
                }
            }
        }
        throw lastException ?: Exception("Превышено максимальное количество попыток")
    }


    suspend fun registerUser(email: String, password: String) {
        try {
            withContext(Dispatchers.IO) {
                println("Attempting to register user with email: $email")

                // Проверяем, существует ли пользователь
                val existingUser = supabase.postgrest
                    .from("users")
                    .select {
                        eq("email", email)
                    }
                    .decodeList<UserProfile>()
                    .firstOrNull()

                if (existingUser != null) {
                    throw Exception("Этот email уже зарегистрирован")
                }

                // Регистрация пользователя в Supabase без верификации email
                withRetry {
                    supabase.gotrue.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        // Устанавливаем email как уже подтверждённый
                        data = buildJsonObject {
                            put("email_verified", true)
                        }
                    }
                }

                // Сразу выполняем вход
                withRetry {
                    supabase.gotrue.loginWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }

                // Получаем ID пользователя
                val session = supabase.gotrue.currentSessionOrNull()
                val userId = session?.user?.id
                    ?: throw Exception("Не удалось получить ID пользователя")

                // Сохраняем данные пользователя
                insertUserData(userId, email)

                println("User registered successfully with email: $email and ID: $userId")
            }
        } catch (e: RestException) {
            when {
                e.message?.contains("User already registered") == true -> {
                    throw Exception("Этот email уже зарегистрирован")
                }
                e.message?.contains("Too Many Requests") == true -> {
                    throw Exception("Пожалуйста, попробуйте позже")
                }
                else -> {
                    throw Exception("подтвердите почту и возращайтесь: ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw Exception("подтвердите почту: ${e.message}")
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

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
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

    suspend fun getUserProfile(userId: String): UserProfile {
        return withContext(Dispatchers.IO) {
            withRetry {
                try {
                    val response = supabase.postgrest
                        .from("users")
                        .select() {
                            eq("user_id", userId)
                        }
                        .decodeList<UserProfile>()
                        .firstOrNull() ?: throw Exception("Профиль пользователя не найден")

                    response
                } catch (e: SerializationException) {
                    Log.e("DatabaseHelper", "Serialization error: ${e.message}")
                    throw Exception("Ошибка при чтении данных пользователя: ${e.message}")
                } catch (e: Exception) {
                    Log.e("DatabaseHelper", "Error getting user profile: ${e.message}")
                    throw e
                }
            }
        }
    }
    suspend fun deleteUserData(userId: String) {
        withContext(Dispatchers.IO) {
            withRetry {
                try {
                    // Сначала выходим из системы
                    supabase.gotrue.logout()

                    // Затем удаляем данные
                    supabase.postgrest
                        .from("UserSettings")
                        .delete {
                            eq("user_id", userId)
                        }

                    supabase.postgrest
                        .from("users")
                        .delete {
                            eq("user_id", userId)
                        }

                    // В конце удаляем пользователя через RPC
                    val params = buildJsonObject {
                        put("user_id", userId)
                    }

                    supabase.postgrest
                        .rpc("delete_auth_user", params)

                } catch (e: Exception) {
                    Log.e("DatabaseHelper", "Error deleting user: ${e.message}")
                    throw Exception("Ошибка при удалении аккаунта: ${e.message}")
                }
            }
        }
    }
    @Serializable
    data class DeleteUserResponse(
        val success: Boolean = true
    )


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