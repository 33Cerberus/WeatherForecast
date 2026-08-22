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

    if (apiKey == null) {
        System.err.println(".env file should contain WEATHER_API_KEY=your_api_key property")
        return@runBlocking
    }

    val cities = listOf("Chisinau", "Madrid", "Kyiv", "Amsterdam")

    val results = cities.map { city ->
        async {
            try {
                city to api.getForecast(city, 2, apiKey)
            } catch (e: Exception) {
                city to null as ForecastResponse?
            }
        }
    }.awaitAll()

    printHeader()

    for (result in results) {
        printResult(result)
    }
}

fun printHeader()
{
    val headerLine = String.format(
        "%-12s %-12s %-8s %-8s %-10s %-10s %-6s",
        "City", "Date", "Min C", "Max C", "Humidity%", "Wind kph", "Dir",
    )
    println(headerLine)
}

fun printResult(result: Pair<String, ForecastResponse?>) {
    val location = result.first
    val forecastResponse = result.second

    if (forecastResponse == null) {
        val line = String.format(
            "%-12s %-12s %-8s %-8s %-10s %-10s %-6s",
            location, "No data", "No data", "No data", "No data", "No data", "No data"
        )
        println(line)
        return
    }

    val forecast = forecastResponse.forecast
    val tomorrow = forecast.forecastday[1]

    val (date, day, hours) = tomorrow
    val (mintemp_c, maxtemp_c, avghumidity, maxwind_kph) = day

    val groupedHours = hours.groupingBy { it.wind_dir }
    val dirCounts = groupedHours.eachCount()
    val topEntry = dirCounts.maxByOrNull { it.value }
    val mostCommonDir = topEntry?.key

    val finalDir = mostCommonDir ?: "No data"

    val line = String.format(
        "%-12s %-12s %-8.1f %-8.1f %-10.1f %-10.1f %-6s",
        location, date, mintemp_c, maxtemp_c, avghumidity, maxwind_kph, finalDir
    )
    println(line)
}