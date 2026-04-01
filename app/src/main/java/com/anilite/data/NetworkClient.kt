package com.anilite.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkClient {

    private val jsonConfiguration = Json {
        ignoreUnknownKeys = true      // Prevents crashes from unknown fields
        isLenient = true              // Allows minor JSON format issues
        coerceInputValues = true      // Helps with type coercion
        prettyPrint = false           // Set to false in production (true only for debugging)
    }

    val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(jsonConfiguration)
            }

            install(Logging) {
                level = LogLevel.BODY     // Good for debugging API responses
            }

            // Optional but recommended: Timeouts
            engine {
                config {
                    import kotlin.time.Duration.Companion.seconds   // ← Add this at the top if not already there

// Then change the timeouts like this:
.connectTimeout(30.seconds)
.readTimeout(30.seconds)
.writeTimeout(30.seconds)
                }
            }
        }
    }
}
