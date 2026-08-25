package io.github.cerberus33.weather

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import io.github.cdimascio.dotenv.dotenv
import io.github.cerberus33.weather.api.createWeatherClient
import io.github.cerberus33.weather.api.fetchForecasts
import io.github.cerberus33.weather.mapper.buildRow
import io.github.cerberus33.weather.output.printHeader
import io.github.cerberus33.weather.output.printRow
import io.github.cerberus33.weather.time.resolveTargetDate

fun main(): Unit = runBlocking {
    val apiKey = dotenv { ignoreIfMissing = true }["WEATHER_API_KEY"] ?: System.getenv("WEATHER_API_KEY")

    if (apiKey.isNullOrBlank()) {
        System.err.println(
            "WEATHER_API_KEY is not set. Add it to a .env file in the project root or export it as an environment variable."
        )
        exitProcess(1)
    }

    createWeatherClient().use { weatherClient ->
        val cities = listOf("Chisinau", "Madrid", "Kyiv", "Amsterdam")
        val tomorrowDate = resolveTargetDate()

        val results = fetchForecasts(weatherClient.api, cities, apiKey)

        printHeader()
        for (result in results) {
            printRow(buildRow(result, tomorrowDate))
        }
    }
}