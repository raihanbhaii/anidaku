package com.anilite.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("hianime/home")
    suspend fun getHome(): HomeResponse

    @GET("hianime/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("hianime/search/advanced")
    suspend fun searchAdvanced(
        @Query("q") query: String,
        @Query("genres") genres: String? = null,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("hianime/anime/{animeId}")
    suspend fun getAnimeDetail(@Path("animeId") animeId: String): AnimeDetailResponse

    @GET("hianime/anime/{animeId}/episodes")
    suspend fun getEpisodes(@Path("animeId") animeId: String): EpisodesResponse

    @GET("hianime/episode/servers")
    suspend fun getServers(@Query("animeEpisodeId") animeEpisodeId: String): ServersResponse

    @GET("hianime/episode/sources")
    suspend fun getSources(
        @Query("animeEpisodeId") animeEpisodeId: String,
        @Query("ep") ep: String,
        @Query("server") server: String = "hd-1",
        @Query("category") category: String = "sub"
    ): SourcesResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://anidaku-api.vercel.app/api/v2/"

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
