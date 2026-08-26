package io.github.cerberus33.weather.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class TargetDateTest {

    @Test
    fun `resolves tomorrow relative to the given clock`() {
        val fixedNow = Instant.parse("2026-08-24T10:00:00Z")
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)

        val result = resolveTomorrow(clock)

        assertEquals(LocalDate.parse("2026-08-25"), result)
    }
}