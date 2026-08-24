package org.example
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

fun createWeatherApi(): WeatherApi {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.weatherapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(WeatherApi::class.java)
}

suspend fun fetchForecasts(api: WeatherApi, cities: List<String>, apiKey: String): List<Pair<String, ForecastResponse?>> = coroutineScope {
    cities.map { city ->
        async {
            try {
                city to api.getForecast(city, 2, apiKey)
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: Exception) {
                System.err.println("Failed to fetch forecast for $city: ${e.message}")
                city to null as ForecastResponse?
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