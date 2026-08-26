package io.github.cerberus33.weather.api

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WeatherApiTest {

    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `returns a parsed response for a valid API reply`() = runBlocking {
        val validJson = """
            {
              "location": { "name": "Kyiv" },
              "forecast": {
                "forecastday": [{
                  "date": "2026-08-27",
                  "day": {
                    "mintemp_c": 14.7, "maxtemp_c": 23.9,
                    "avghumidity": 39.0, "maxwind_kph": 22.0
                  },
                  "hour": [{ "time": "2026-08-27 00:00", "wind_dir": "NW" }]
                }]
              }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setBody(validJson).setResponseCode(200))

        val client = createWeatherClient(baseUrl = server.url("/").toString())
        val results = fetchForecasts(client.api, listOf("Kyiv"), "fake-key", days = 3)

        val (city, response) = results.single()
        assertNotNull(response)
        assertEquals("Kyiv", response.location.name)
        assertEquals(14.7, response.forecast.forecastDays[0].day.minTempC)
    }

    @Test
    fun `degrades to null when the API returns a malformed body`() = runBlocking {
        server.enqueue(MockResponse().setBody("{ not valid json").setResponseCode(200))

        val client = createWeatherClient(baseUrl = server.url("/").toString())
        val results = fetchForecasts(client.api, listOf("Kyiv"), "fake-key", days = 3)

        val (_, response) = results.single()
        assertNull(response)
    }
}