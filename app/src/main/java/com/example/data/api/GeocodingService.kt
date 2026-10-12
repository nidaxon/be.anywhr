package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NominatimLocation(
    @Json(name = "place_id") val placeId: Long = 0,
    @Json(name = "lat") val latStr: String,
    @Json(name = "lon") val lonStr: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "importance") val importance: Double? = null
) {
    val latitude: Double get() = latStr.toDoubleOrNull() ?: 0.0
    val longitude: Double get() = lonStr.toDoubleOrNull() ?: 0.0
}

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Header("User-Agent") userAgent: String = "GeoShift-AndroidApp"
    ): List<NominatimLocation>
}

object GeocodingClient {
    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val api: NominatimApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NominatimApi::class.java)

    // Curated popular locations for instant offline access and quick hopping
    val popularLocations = listOf(
        PopularLocation("Monas (National Monument)", "Jakarta, Indonesia", -6.175392, 106.827153, "🇮🇩"),
        PopularLocation("Eiffel Tower", "Paris, France", 48.858370, 2.294481, "🇫🇷"),
        PopularLocation("Times Square", "New York, USA", 40.758896, -73.985130, "🇺🇸"),
        PopularLocation("Shibuya Crossing", "Tokyo, Japan", 35.659516, 139.700547, "🇯🇵"),
        PopularLocation("Sydney Opera House", "Sydney, Australia", -33.856784, 151.215297, "🇦🇺"),
        PopularLocation("Colosseum", "Rome, Italy", 41.890210, 12.492231, "🇮🇹"),
        PopularLocation("Santa Monica Pier", "California, USA", 34.009914, -118.496006, "🇺🇸"),
        PopularLocation("Marina Bay Sands", "Singapore", 1.283375, 103.860725, "🇸🇬"),
        PopularLocation("London Eye", "London, UK", 51.503324, -0.119543, "🇬🇧"),
        PopularLocation("Burj Khalifa", "Dubai, UAE", 25.197197, 55.274376, "🇦🇪"),
        PopularLocation("Taipei 101", "Taipei, Taiwan", 25.033964, 121.564472, "🇹🇼"),
        PopularLocation("Borobudur Temple", "Magelang, Indonesia", -7.607874, 110.203751, "🇮🇩")
    )
}

data class PopularLocation(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val emoji: String
)
