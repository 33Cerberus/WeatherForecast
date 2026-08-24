package io.github.cerberus33.weather.model

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    val location: Location,
    val forecast: Forecast
)

data class Location(
    val name: String
)

data class Forecast(
    @SerializedName("forecastday") val forecastDays: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: Day,
    @SerializedName("hour") val hours: List<Hour>
)

data class Day(
    @SerializedName("mintemp_c") val minTempC: Double,
    @SerializedName("maxtemp_c") val maxTempC: Double,
    @SerializedName("avghumidity") val avgHumidity: Double,
    @SerializedName("maxwind_kph") val maxWindKph: Double
)

data class Hour(
    val time: String,
    @SerializedName("wind_dir") val windDir: String
)