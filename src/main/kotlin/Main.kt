package org.example
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import io.github.cdimascio.dotenv.dotenv

fun main(): Unit = runBlocking {
    val api = createWeatherApi()
    val apiKey = dotenv()["WEATHER_API_KEY"]

    if (apiKey == null) {
        System.err.println(".env file should contain WEATHER_API_KEY=your_api_key property")
        return@runBlocking
    }

    val cities = listOf("Chisinau", "Madrid", "Kyiv", "Amsterdam")
    val tomorrowDate = LocalDate.now().plusDays(1).toString()

    val results = fetchForecasts(api, cities, apiKey)

    printHeader()
    for (result in results) {
        printRow(buildRow(result, tomorrowDate))
    }
}