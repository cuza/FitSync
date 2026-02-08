package dev.cuza.FitSync.domain.sync

import dev.cuza.FitSync.domain.model.WorkoutSample
import dev.cuza.FitSync.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant

class SessionHashCalculatorTest {

    private val calculator = SessionHashCalculator()

    @Test
    fun `hash is stable regardless of sample order`() {
        val sessionA = baseSession(
            samples = listOf(
                WorkoutSample(timestamp = t("2026-01-01T10:05:00Z"), heartRateBpm = 140),
                WorkoutSample(timestamp = t("2026-01-01T10:01:00Z"), heartRateBpm = 130),
            ),
        )
        val sessionB = baseSession(
            samples = listOf(
                WorkoutSample(timestamp = t("2026-01-01T10:01:00Z"), heartRateBpm = 130),
                WorkoutSample(timestamp = t("2026-01-01T10:05:00Z"), heartRateBpm = 140),
            ),
        )

        assertEquals(calculator.hash(sessionA), calculator.hash(sessionB))
    }

    @Test
    fun `hash changes when stream data changes`() {
        val original = baseSession(
            samples = listOf(
                WorkoutSample(timestamp = t("2026-01-01T10:01:00Z"), heartRateBpm = 130),
            ),
        )
        val changed = baseSession(
            samples = listOf(
                WorkoutSample(timestamp = t("2026-01-01T10:01:00Z"), heartRateBpm = 150),
            ),
        )

        assertNotEquals(calculator.hash(original), calculator.hash(changed))
    }

    private fun baseSession(samples: List<WorkoutSample>): WorkoutSession {
        return WorkoutSession(
            healthConnectSessionId = "session-1",
            title = "Workout",
            startTime = t("2026-01-01T10:00:00Z"),
            endTime = t("2026-01-01T10:10:00Z"),
            exerciseType = 1,
            totalDistanceMeters = 1000.0,
            totalCaloriesKcal = 120.0,
            totalSteps = 2000,
            samples = samples,
        )
    }

    private fun t(iso: String): Instant = Instant.parse(iso)
}
