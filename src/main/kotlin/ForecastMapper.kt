package org.example
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

fun buildRow(result: Pair<String, ForecastResponse?>, tomorrowDate: String): CityWeatherRow{
    val givenLocation = result.first
    val forecastResponse = result.second

    if (forecastResponse == null) {
        return CityWeatherRow(givenLocation, tomorrowDate, NO_DATA, NO_DATA, NO_DATA, NO_DATA, NO_DATA)
    }

    val receivedLocation = forecastResponse.location.name

    val forecast = forecastResponse.forecast
    val tomorrow = findForecastForDate(forecast.forecastDays, tomorrowDate)

    if (tomorrow == null) {
        return CityWeatherRow(receivedLocation, tomorrowDate, NO_DATA,NO_DATA, NO_DATA, NO_DATA, NO_DATA)
    }

    val date = tomorrow.date
    val day = tomorrow.day
    val hours = tomorrow.hours

    val minTempC = day.minTempC
    val maxTempC = day.maxTempC
    val avgHumidity = day.avgHumidity
    val maxWindKph = day.maxWindKph

    val finalDir = findMostCommonWindDirection(hours) ?: NO_DATA

    return CityWeatherRow(receivedLocation, date,
        String.format(Locale.US, "%.1f", minTempC),
        String.format(Locale.US, "%.1f", maxTempC),
        String.format(Locale.US, "%.0f", avgHumidity),
        String.format(Locale.US, "%.1f", maxWindKph),
        finalDir)
}

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