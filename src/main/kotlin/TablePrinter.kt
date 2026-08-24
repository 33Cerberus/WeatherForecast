package org.example

import java.util.Locale

const val TABLE_FORMAT = "%-12s %-12s %-8s %-8s %-10s %-10s %-6s"

fun printHeader() {
    printRow(CityWeatherRow("City", "Date", "Min C", "Max C", "Humidity%", "Wind kph", "Dir"))
}

fun printRow(row: CityWeatherRow) {
    println(String.format(Locale.US, TABLE_FORMAT, row.city, row.date, row.minTemp, row.maxTemp, row.humidity, row.windSpeed, row.windDir))
}