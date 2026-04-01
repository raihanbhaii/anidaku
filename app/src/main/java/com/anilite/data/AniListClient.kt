package com.anilite.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AniListClient {

    private const val URL = "https://graphql.anilist.co"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun query(query: String, variables: Map<String, Any> = emptyMap()): JSONObject {
        val body = JSONObject()
        body.put("query", query)
        body.put("variables", JSONObject(variables))

        val request = Request.Builder()
            .url(URL)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: "{}"
        return JSONObject(responseBody).getJSONObject("data")
    }
}
