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
            day = Day(minTempC = 5.0, maxTempC = 10.0, avgHumidity = 25.0, maxWindKph = 7.5),
            hours = emptyList()
        )

        val days = listOf(
            ForecastDay(
                date = "2026-08-24",
                day = Day(minTempC = 10.0, maxTempC = 20.0, avgHumidity = 50.0, maxWindKph = 15.0),
                hours = emptyList()
            ),
            expectedDay,
            ForecastDay(
                date = "2026-08-26",
                day = Day(minTempC = 3.0, maxTempC = 7.0, avgHumidity = 13.0, maxWindKph = 5.0),
                hours = emptyList()
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
                day = Day(minTempC = 10.0, maxTempC = 20.0, avgHumidity = 50.0, maxWindKph = 15.0),
                hours = emptyList()
            ),
            ForecastDay(
                date = "2026-08-25",
                day = Day(minTempC = 15.0, maxTempC = 25.0, avgHumidity = 10.0, maxWindKph = 12.0),
                hours = emptyList()
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
                        day = Day(minTempC = 10.0, maxTempC = 20.0, avgHumidity = 50.0, maxWindKph = 15.0),
                        hours = emptyList()
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
                humidity = "39.0",
                windSpeed = "22.0",
                windDir = "NW"
            ),
            row
        )
    }
}