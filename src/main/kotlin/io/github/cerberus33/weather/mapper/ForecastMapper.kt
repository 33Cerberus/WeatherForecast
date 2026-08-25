package io.github.cerberus33.weather.mapper

import io.github.cerberus33.weather.model.ForecastDay
import io.github.cerberus33.weather.model.ForecastResponse
import io.github.cerberus33.weather.model.Hour
import java.time.LocalDate
import java.util.Locale

private const val NO_DATA = "No data"

data class CityWeatherRow(
    val city: String,
    val date: String,
    val minTemp: String,
    val maxTemp: String,
    val humidity: String,
    val windSpeed: String,
    val windDir: String
)

fun buildRow(result: Pair<String, ForecastResponse?>, targetDate: LocalDate): CityWeatherRow {
    val (queriedCity, response) = result
    val dateLabel = targetDate.toString()

    if (response == null) return emptyRow(queriedCity, dateLabel)

    val city = response.location.name.takeIf { it.isNotBlank() } ?: queriedCity
    val tomorrow = findForecastForDate(response.forecast.forecastDays, targetDate)
        ?: return emptyRow(city, dateLabel)

    return CityWeatherRow(
        city = city,
        date = tomorrow.date,
        minTemp = format(tomorrow.day.minTempC),
        maxTemp = format(tomorrow.day.maxTempC),
        humidity = format(tomorrow.day.avgHumidity, decimals = 0),
        windSpeed = format(tomorrow.day.maxWindKph),
        windDir = findMostCommonWindDirection(tomorrow.hours) ?: NO_DATA
    )
}

private fun format(value: Double, decimals: Int = 1): String =
    String.format(Locale.US, "%.${decimals}f", value)

private fun emptyRow(city: String, date: String) =
    CityWeatherRow(city, date, NO_DATA, NO_DATA, NO_DATA, NO_DATA, NO_DATA)

fun findMostCommonWindDirection(hours: List<Hour>): String? =
    hours.groupingBy { it.windDir }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

fun findForecastForDate(days: List<ForecastDay>, date: LocalDate): ForecastDay? =
    days.firstOrNull { it.date == date.toString() }