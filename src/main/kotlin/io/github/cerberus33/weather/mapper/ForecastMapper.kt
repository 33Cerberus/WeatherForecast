package io.github.cerberus33.weather.mapper

import io.github.cerberus33.weather.model.ForecastDay
import io.github.cerberus33.weather.model.ForecastResponse
import io.github.cerberus33.weather.model.Hour
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

fun buildRow(result: Pair<String, ForecastResponse?>, tomorrowDate: String): CityWeatherRow {
    val givenLocation = result.first
    val forecastResponse = result.second

    if (forecastResponse == null) {
        return CityWeatherRow(givenLocation, tomorrowDate, NO_DATA, NO_DATA, NO_DATA, NO_DATA, NO_DATA)
    }

    val receivedLocation = forecastResponse.location.name

    val forecast = forecastResponse.forecast
    val tomorrow = findForecastForDate(forecast.forecastDays, tomorrowDate)

    if (tomorrow == null) {
        return CityWeatherRow(receivedLocation, tomorrowDate, NO_DATA, NO_DATA, NO_DATA, NO_DATA, NO_DATA)
    }

    val finalDir = findMostCommonWindDirection(tomorrow.hours) ?: NO_DATA

    return CityWeatherRow(
        receivedLocation,
        tomorrow.date,
        format(tomorrow.day.minTempC),
        format(tomorrow.day.maxTempC),
        format(tomorrow.day.avgHumidity, 0),
        format(tomorrow.day.maxWindKph),
        finalDir
    )
}

private fun format(value: Double, decimals: Int = 1) = String.format(Locale.US, "%.${decimals}f", value)

fun findMostCommonWindDirection(hours: List<Hour>): String? {
    val groupedHours = hours.groupingBy { it.windDir }
    val dirCounts = groupedHours.eachCount()
    val topEntry = dirCounts.maxByOrNull { it.value }
    val mostCommonDir = topEntry?.key
    return mostCommonDir
}

fun findForecastForDate(days: List<ForecastDay>, date: String): ForecastDay? {
    return days.firstOrNull { it.date == date }
}