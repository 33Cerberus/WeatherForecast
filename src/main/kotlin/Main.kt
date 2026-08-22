package org.example
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

fun main(): Unit = runBlocking {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.weatherapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(WeatherApi::class.java)

    val dotenv = dotenv()
    val apiKey = dotenv["WEATHER_API_KEY"]

    val cities = listOf("Chisinau", "Madrid", "Kyiv", "Amsterdam")

    val results = cities.map { city ->
        async { city to api.getForecast(city, 2, apiKey) }
    }.awaitAll()

    val headerLine = String.format(
        "%-12s %-12s %-8s %-8s %-10s %-10s %-6s",
        "City", "Date", "Min C", "Max C", "Humidity%", "Wind kph", "Dir",
    )
    println(headerLine)

    for (result in results) {
        printResult(result)
    }
}

fun printResult(result: Pair<String, ForecastResponse>) {
    val location = result.first

    val forecastResponse = result.second
    val forecast = forecastResponse.forecast

    val tomorrow = forecast.forecastday[1]
    val (date, day, hours) = tomorrow

    val (mintemp_c, maxtemp_c, avghumidity, maxwind_kph) = day
    val noonHour = hours.firstOrNull { it.time.endsWith("12:00") }
    val wind_dir = noonHour?.wind_dir

    val line = String.format(
        "%-12s %-12s %-8.1f %-8.1f %-10.1f %-10.1f %-6s",
        location, date, mintemp_c, maxtemp_c, avghumidity, maxwind_kph, wind_dir
    )
    println(line)
}