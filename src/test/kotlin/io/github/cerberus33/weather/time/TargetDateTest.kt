package io.github.cerberus33.weather.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TargetDateTest {

    @Test
    fun `resolves today relative to the given clock`() {
        val clock = Clock.fixed(Instant.parse("2026-08-24T10:00:00Z"), ZoneOffset.UTC)

        val result = resolveToday(clock)

        assertEquals(LocalDate.parse("2026-08-24"), result)
    }

    @Test
    fun `resolves a range of consecutive dates starting from an offset`() {
        val clock = Clock.fixed(Instant.parse("2026-08-24T10:00:00Z"), ZoneOffset.UTC)

        val result = resolveDateRange(startDaysFromNow = 1, days = 3, clock = clock)

        assertEquals(
            listOf(
                LocalDate.parse("2026-08-25"),
                LocalDate.parse("2026-08-26"),
                LocalDate.parse("2026-08-27")
            ),
            result
        )
    }

    @Test
    fun `resolves seven consecutive days starting tomorrow`() {
        val clock = Clock.fixed(Instant.parse("2026-08-24T10:00:00Z"), ZoneOffset.UTC)

        val result = resolveWeekAhead(clock)

        assertEquals(7, result.size)
        assertEquals(LocalDate.parse("2026-08-25"), result.first())
        assertEquals(LocalDate.parse("2026-08-31"), result.last())
    }

    @Test
    fun `computes forecast days needed to cover the furthest target date`() {
        val clock = Clock.fixed(Instant.parse("2026-08-24T10:00:00Z"), ZoneOffset.UTC)
        val targetDates = listOf(LocalDate.parse("2026-08-25"), LocalDate.parse("2026-08-27"))

        val result = resolveForecastDays(targetDates, clock)

        assertEquals(5, result)
    }

    @Test
    fun `throws when no target dates are given`() {
        assertFailsWith<IllegalArgumentException> {
            resolveForecastDays(emptyList())
        }
    }
}