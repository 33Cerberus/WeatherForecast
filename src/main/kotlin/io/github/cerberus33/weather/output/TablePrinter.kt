package io.github.cerberus33.weather.output

import io.github.cerberus33.weather.mapper.CityWeatherRow
import java.util.Locale

private const val TABLE_FORMAT = "%-12s %-12s %-8s %-8s %-10s %-10s %-6s"
private const val CITY_WIDTH = 12

fun printHeader() {
    printFormattedRow("City", "Date", "Min C", "Max C", "Humidity%", "Wind kph", "Dir")
}

fun printRow(row: CityWeatherRow) {
    printFormattedRow(
        truncate(row.city, CITY_WIDTH),
        row.date,
        row.minTemp,
        row.maxTemp,
        row.humidity,
        row.windSpeed,
        row.windDir
    )
}

private fun printFormattedRow(
    city: String, date: String, minTemp: String, maxTemp: String,
    humidity: String, windSpeed: String, windDir: String
) {
    println(String.format(Locale.US, TABLE_FORMAT, city, date, minTemp, maxTemp, humidity, windSpeed, windDir))
}

private fun truncate(value: String, maxLength: Int): String =
    if (value.length <= maxLength) value else value.take(maxLength - 1) + "…"