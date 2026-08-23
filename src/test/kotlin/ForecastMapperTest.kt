package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForecastMapperTest {

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
        val expectedDay = ForecastDay(
            date = requestedDate,
            day = Day(mintemp_c = 5.0, maxtemp_c = 10.0, avghumidity = 25.0, maxwind_kph = 7.5),
            hour = emptyList()
        )

        val days = listOf(
            ForecastDay(
                date = "2026-08-24",
                day = Day(mintemp_c = 10.0, maxtemp_c = 20.0, avghumidity = 50.0, maxwind_kph = 15.0),
                hour = emptyList()
            ),
            expectedDay,
            ForecastDay(
                date = "2026-08-26",
                day = Day(mintemp_c = 3.0, maxtemp_c = 7.0, avghumidity = 13.0, maxwind_kph = 5.0),
                hour = emptyList()
            )
        )

        val result = findForecastForDate(days, requestedDate)

        assertEquals(expectedDay, result)
    }

    @Test
    fun `returns null when the requested date is absent`() {
        val days = listOf(
            ForecastDay(
                date = "2026-08-24",
                day = Day(mintemp_c = 10.0, maxtemp_c = 20.0, avghumidity = 50.0, maxwind_kph = 15.0),
                hour = emptyList()
            ),
            ForecastDay(
                date = "2026-08-25",
                day = Day(mintemp_c = 15.0, maxtemp_c = 25.0, avghumidity = 10.0, maxwind_kph = 12.0),
                hour = emptyList()
            )
        )

        val result = findForecastForDate(days, "2026-08-01")

        assertNull(result)
    }

    @Test
    fun `builds a placeholder row when the request failed`() {
        val row = buildRow("Kyiv" to null, "2026-08-24")

        assertEquals(
            CityWeatherRow(
                city = "Kyiv",
                date = "No data",
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
        val response = ForecastResponse(
            location = Location("Kyiv"),
            forecast = Forecast(
                listOf(
                    ForecastDay(
                        date = "2026-08-25",
                        day = Day(mintemp_c = 10.0, maxtemp_c = 20.0, avghumidity = 50.0, maxwind_kph = 15.0),
                        hour = emptyList()
                    )
                )
            )
        )

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
                        day = Day(mintemp_c = 14.7, maxtemp_c = 23.9, avghumidity = 39.0, maxwind_kph = 22.0),
                        hour = listOf(
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
                humidity = "39.0",
                windSpeed = "22.0",
                windDir = "NW"
            ),
            row
        )
    }
}