package com.anilite.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds
import java.time.Duration as JavaDuration   // Alias to avoid conflict

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

            // Fix: OkHttp engine expects java.time.Duration, not kotlin.time.Duration
            engine {
                config {
                    connectTimeout(30.seconds.toJavaDuration())
                    readTimeout(30.seconds.toJavaDuration())
                    writeTimeout(30.seconds.toJavaDuration())
                }
            }
        }
    }

    // Extension function to convert kotlin.time.Duration → java.time.Duration
    private fun kotlin.time.Duration.toJavaDuration(): JavaDuration {
        return JavaDuration.ofNanos(this.inWholeNanoseconds)
    }
}
