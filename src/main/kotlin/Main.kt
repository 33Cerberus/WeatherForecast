package org.example
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import io.github.cdimascio.dotenv.dotenv

fun main(): Unit = runBlocking {
    val (api, client) = createWeatherApi()

    val apiKey = dotenv { ignoreIfMissing = true }["WEATHER_API_KEY"] ?: System.getenv("WEATHER_API_KEY")

    if (apiKey == null) {
        System.err.println(
            "WEATHER_API_KEY is not set. Add it to a .env file in the project root or export it as an environment variable."
        )
        exitProcess(1)
    }

    val cities = listOf("Chisinau", "Madrid", "Kyiv", "Amsterdam")
    val tomorrowDate = LocalDate.now().plusDays(1).toString()

    val results = fetchForecasts(api, cities, apiKey)

    printHeader()
    for (result in results) {
        printRow(buildRow(result, tomorrowDate))
    }

    client.dispatcher().executorService().shutdown()
    client.connectionPool().evictAll()
}