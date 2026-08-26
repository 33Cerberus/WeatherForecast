package io.github.cerberus33.weather.time

import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun resolveDate(daysFromNow: Long, clock: Clock = Clock.systemDefaultZone()): LocalDate =
    LocalDate.now(clock).plusDays(daysFromNow)

fun resolveToday(clock: Clock = Clock.systemDefaultZone()): LocalDate =
    resolveDate(0, clock)

fun resolveTomorrow(clock: Clock = Clock.systemDefaultZone()): LocalDate =
    resolveDate(1, clock)

fun resolveDateRange(startDaysFromNow: Long, days: Int, clock: Clock = Clock.systemDefaultZone()): List<LocalDate> =
    (0 until days).map { resolveDate(startDaysFromNow + it, clock) }

fun resolveWeekAhead(clock: Clock = Clock.systemDefaultZone()): List<LocalDate> =
    resolveDateRange(startDaysFromNow = 1, days = 7, clock = clock)

fun resolveForecastDays(targetDates: List<LocalDate>, today: LocalDate = LocalDate.now()): Int {
    val daysNeeded = ChronoUnit.DAYS.between(today, targetDates.max())
    return (daysNeeded + 2).toInt()
}