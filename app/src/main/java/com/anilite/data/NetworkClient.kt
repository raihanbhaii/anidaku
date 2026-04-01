package com.anilite.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds   // ← This is the correct import (added at top)

object NetworkClient {

    private val jsonConfiguration = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        prettyPrint = false
    }

    val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(jsonConfiguration)
            }

            install(Logging) {
                level = LogLevel.BODY
            }

            // Timeouts - Fixed correctly
            engine {
                config {
                    connectTimeout(30.seconds)
                    readTimeout(30.seconds)
                    writeTimeout(30.seconds)
                }
            }
        }
    }
}
