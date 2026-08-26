package io.github.cerberus33.weather.output

import io.github.cerberus33.weather.mapper.CityWeatherRow
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class TablePrinterTest {

    private fun captureOutput(action: () -> Unit): String {
        val originalOut = System.out
        val captured = ByteArrayOutputStream()
        System.setOut(PrintStream(captured))
        try {
            action()
        } finally {
            System.setOut(originalOut)
        }
        return captured.toString().trim()
    }

    @Test
    fun `prints the header with expected column labels`() {
        val output = captureOutput { printHeader() }

        assertEquals(
            "City         Date         Min C    Max C    Humidity%  Wind kph   Dir",
            output
        )
    }

    @Test
    fun `truncates a city name longer than the column width`() {
        val row = CityWeatherRow(
            city = "Verylongcityname",
            date = "2026-08-24",
            minTemp = "14.7",
            maxTemp = "23.9",
            humidity = "39",
            windSpeed = "22.0",
            windDir = "NW"
        )

        val output = captureOutput { printRow(row) }

        assertTrue(output.startsWith("Verylongc..."), "Expected truncated name, got: $output")
    }

    @Test
    fun `does not truncate a city name that fits exactly`() {
        val row = CityWeatherRow(
            city = "Copenhagen",
            date = "2026-08-24",
            minTemp = "14.7",
            maxTemp = "23.9",
            humidity = "39",
            windSpeed = "22.0",
            windDir = "NW"
        )

        val output = captureOutput { printRow(row) }

        assertTrue(output.startsWith("Copenhagen"), "Expected full name, got: $output")
    }
}