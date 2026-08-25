package io.github.cerberus33.weather.mapper

import com.google.gson.Gson
import io.github.cerberus33.weather.model.Day
import io.github.cerberus33.weather.model.Forecast
import io.github.cerberus33.weather.model.ForecastDay
import io.github.cerberus33.weather.model.ForecastResponse
import io.github.cerberus33.weather.model.Hour
import io.github.cerberus33.weather.model.Location
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForecastMapperTest {
    private fun day(date: String, hours: List<Hour> = emptyList()) =
        ForecastDay(date, Day(14.7, 23.9, 39.0, 22.0), hours)

    private fun response(vararg days: ForecastDay, name: String = "Kyiv") =
        ForecastResponse(Location(name), Forecast(days.toList()))

    @Test
    fun `returns the direction that occurs most often`() {
        val hours = listOf(
            Hour("2026-08-24 00:00", "N"),
            Hour("2026-08-24 01:00", "N"),
            Hour("2026-08-24 02:00", "SW")
        )

        val result = findMostCommonWindDirection(hours)

        assertEquals("N", result)
    }

    @Test
    fun `returns null when there are no hours`() {
        val result = findMostCommonWindDirection(emptyList())

        assertNull(result)
    }

    @Test
    fun `returns the earliest direction when counts are tied`() {
        val hours = listOf(
            Hour("2026-08-24 00:00", "N"),
            Hour("2026-08-24 01:00", "S"),
            Hour("2026-08-24 02:00", "W")
        )

        val result = findMostCommonWindDirection(hours)

        assertEquals("N", result)
    }

    @Test
    fun `returns the forecast matching the requested date`() {
        val requestedDate = "2026-08-25"
        val expectedDay = day(requestedDate)

        val days = listOf(day("2026-08-24"), expectedDay, day("2026-08-26"))

        val result = findForecastForDate(days, requestedDate)

        assertEquals(expectedDay, result)
    }

    @Test
    fun `returns null when the requested date is absent`() {
        val days = listOf(day("2026-08-24"), day("2026-08-25"))

        val result = findForecastForDate(days, "2026-08-01")

        assertNull(result)
    }

    @Test
    fun `builds a placeholder row when the request failed`() {
        val row = buildRow("Kyiv" to null, "2026-08-24")

        assertEquals(
            CityWeatherRow(
                city = "Kyiv",
                date = "2026-08-24",
                minTemp = "No data",
                maxTemp = "No data",
                humidity = "No data",
                windSpeed = "No data",
                windDir = "No data"
            ),
            row
        )
    }

    @Test
    fun `builds a placeholder row when the response lacks the requested date`() {
        val response = response(day("2026-08-25"))

        val row = buildRow("Kyiv" to response, "2026-08-24")

        assertEquals(
            CityWeatherRow(
                city = "Kyiv",
                date = "2026-08-24",
                minTemp = "No data",
                maxTemp = "No data",
                humidity = "No data",
                windSpeed = "No data",
                windDir = "No data"
            ),
            row
        )
    }

    @Test
    fun `builds a complete row from a valid response`() {
        val response = ForecastResponse(
            location = Location("Kyiv"),
            forecast = Forecast(
                listOf(
                    ForecastDay(
                        date = "2026-08-24",
                        day = Day(minTempC = 14.7, maxTempC = 23.9, avgHumidity = 39.0, maxWindKph = 22.0),
                        hours = listOf(
                            Hour("2026-08-24 00:00", "NW"),
                            Hour("2026-08-24 01:00", "NW"),
                            Hour("2026-08-24 02:00", "S")
                        )
                    )
                )
            )
        )

        val row = buildRow("Kyiv" to response, "2026-08-24")

        assertEquals(
            CityWeatherRow(
                city = "Kyiv",
                date = "2026-08-24",
                minTemp = "14.7",
                maxTemp = "23.9",
                humidity = "39",
                windSpeed = "22.0",
                windDir = "NW"
            ),
            row
        )
    }

    @Test
    fun `uses the location name returned by the API, not the query string`() {
        val response = response(day("2026-08-24"), name = "Kyiv")

        val row = buildRow("kiev" to response, "2026-08-24")

        assertEquals("Kyiv", row.city)
    }

    @Test
    fun `keeps the query string when the request failed`() {
        val row = buildRow("kiev" to null, "2026-08-24")

        assertEquals("kiev", row.city)
    }

    @Test
    fun `deserializes the WeatherAPI response shape`() {
        val json = """
        {
          "location": { "name": "Kyiv" },
          "forecast": {
            "forecastday": [{
              "date": "2026-08-24",
              "day": {
                "mintemp_c": 14.7, "maxtemp_c": 23.9,
                "avghumidity": 39.0, "maxwind_kph": 22.0
              },
              "hour": [{ "time": "2026-08-24 00:00", "wind_dir": "NW" }]
            }]
          }
        }
    """.trimIndent()

        val response = Gson().fromJson(json, ForecastResponse::class.java)

        assertEquals("Kyiv", response.location.name)
        assertEquals(14.7, response.forecast.forecastDays[0].day.minTempC)
        assertEquals("NW", response.forecast.forecastDays[0].hours[0].windDir)
    }
}