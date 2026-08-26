package io.github.cerberus33.weather.api

import io.github.cerberus33.weather.model.ForecastResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private val json = Json { ignoreUnknownKeys = true }

class WeatherClient(val api: WeatherApi, private val client: OkHttpClient) : AutoCloseable {
    override fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

fun createWeatherClient(baseUrl: String = "https://api.weatherapi.com/"): WeatherClient {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    return WeatherClient(retrofit.create(WeatherApi::class.java), client)
}

suspend fun fetchForecasts(
    api: WeatherApi,
    cities: List<String>,
    apiKey: String,
    days: Int
): List<Pair<String, ForecastResponse?>> = coroutineScope {
    cities.map { city ->
        async {
            try {
                city to api.getForecast(city, days, apiKey)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                System.err.println("Failed to fetch forecast for $city: ${e.message}")
                city to null
            }
        }
    }.awaitAll()
}

interface WeatherApi {
    @GET("v1/forecast.json")
    suspend fun getForecast(
        @Query("q") q: String,
        @Query("days") days: Int,
        @Query("key") key: String
    ): ForecastResponse
}