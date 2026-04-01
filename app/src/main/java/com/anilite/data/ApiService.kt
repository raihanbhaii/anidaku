package com.anilite.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApiService {

    @GET("aniwatch/home")
    suspend fun getHome(): HomeResponse

    @GET("aniwatch/search")
    suspend fun search(
        @Query("keyword") query: String,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("aniwatch/anime/{id}")
    suspend fun getAnimeDetail(@Path("id") id: String): AnimeDetailResponse

    @GET("aniwatch/episodes/{id}")
    suspend fun getEpisodes(@Path("id") id: String): EpisodesResponse

    @GET("aniwatch/servers")
    suspend fun getServers(@Query("id") episodeId: String): ServersResponse

    @GET("aniwatch/episode-srcs")
    suspend fun getSources(
        @Query("id") episodeId: String,
        @Query("server") server: String = "hd-1",
        @Query("category") category: String = "sub"
    ): SourcesResponse

    @GET("aniwatch/{category}")
    suspend fun getCategory(
        @Path("category") category: String,
        @Query("page") page: Int = 1
    ): SearchResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://anidexz-api.vercel.app/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}
