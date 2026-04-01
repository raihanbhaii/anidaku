package com.anilite.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

object AniwatchRepository {

    private const val BASE_URL = "https://anidexz-api.vercel.app/aniwatch"

    suspend fun getHome(): HomeResponse {
        return httpClient.get("$BASE_URL/").body()
    }
}
