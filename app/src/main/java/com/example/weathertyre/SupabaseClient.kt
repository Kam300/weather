package com.example.weathertyre

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header

object SupabaseClient {
    private const val SUPABASE_URL = "https://logwaqpwkhlxmwugtknx.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxvZ3dhcXB3a2hseG13dWd0a254Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzczNTE1OTgsImV4cCI6MjA1MjkyNzU5OH0.gjCtB_w1pbiUffDaTblAhdpfYmHUGm3Ei4Fdf5ongjY"

    @OptIn(SupabaseInternal::class)
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(GoTrue)
        install(Postgrest)

        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }

            install(DefaultRequest) {
                header("User-Agent", "WeatherTyre-Android-App")
            }
        }
    }
}