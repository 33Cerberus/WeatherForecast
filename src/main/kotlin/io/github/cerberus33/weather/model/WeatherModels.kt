package io.github.cerberus33.weather.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse(
    val location: Location,
    val forecast: Forecast
)

@Serializable
data class Location(val name: String)

@Serializable
data class Forecast(
    @SerialName("forecastday") val forecastDays: List<ForecastDay>
)

@Serializable
data class ForecastDay(
    val date: String,
    val day: Day,
    @SerialName("hour") val hours: List<Hour>
)

@Serializable
data class Day(
    @SerialName("mintemp_c") val minTempC: Double,
    @SerialName("maxtemp_c") val maxTempC: Double,
    @SerialName("avghumidity") val avgHumidity: Double,
    @SerialName("maxwind_kph") val maxWindKph: Double
)

@Serializable
data class Hour(
    val time: String,
    @SerialName("wind_dir") val windDir: String
)