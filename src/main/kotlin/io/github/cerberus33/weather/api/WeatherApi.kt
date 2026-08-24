package io.github.cerberus33.weather.api

import io.github.cerberus33.weather.model.ForecastResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val FORECAST_DAYS = 3

fun createWeatherApi(): Pair<WeatherApi, OkHttpClient>{
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.weatherapi.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(WeatherApi::class.java) to client
}

suspend fun fetchForecasts(api: WeatherApi, cities: List<String>, apiKey: String): List<Pair<String, ForecastResponse?>> = coroutineScope {
    cities.map { city ->
        async {
            try {
                city to api.getForecast(city, FORECAST_DAYS, apiKey)
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: Exception) {
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