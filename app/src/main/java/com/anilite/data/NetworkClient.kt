package com.anilite.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.Duration   // Use java.time.Duration directly
import kotlin.time.Duration.Companion.seconds

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

            engine {
                config {
                    // Directly use java.time.Duration
                    connectTimeout(Duration.ofSeconds(30))
                    readTimeout(Duration.ofSeconds(30))
                    writeTimeout(Duration.ofSeconds(30))
                }
            }
        }
    }
}
