package io.github.cerberus33.weather.time

import java.time.Clock
import java.time.LocalDate

fun resolveTargetDate(clock: Clock = Clock.systemDefaultZone()): LocalDate =
    LocalDate.now(clock).plusDays(1)