package dev.cuza.FitSync.domain.mapping

import androidx.health.connect.client.records.ExerciseSessionRecord
import dev.cuza.FitSync.domain.model.StravaActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class ExerciseTypeMapperTest {

    private val mapper = ExerciseTypeMapper()

    @Test
    fun `override takes precedence`() {
        val exerciseType = constant("EXERCISE_TYPE_RUNNING") ?: 777777

        val resolved = mapper.resolve(
            exerciseType = exerciseType,
            overrides = mapOf(exerciseType to StravaActivityType.ROWING),
        )

        assertEquals(StravaActivityType.ROWING, resolved)
    }

    @Test
    fun `unknown type falls back to workout`() {
        assertEquals(StravaActivityType.WORKOUT, mapper.resolve(Int.MAX_VALUE, emptyMap()))
        assertEquals("Type 987654", mapper.displayName(987654))
    }

    @Test
    fun `known indoor types map to expected strava types when available`() {
        val mappings = listOf(
            "EXERCISE_TYPE_ROWING" to StravaActivityType.ROWING,
            "EXERCISE_TYPE_ROWING_MACHINE" to StravaActivityType.ROWING,
            "EXERCISE_TYPE_RUNNING_TREADMILL" to StravaActivityType.RUN,
            "EXERCISE_TYPE_ELLIPTICAL" to StravaActivityType.ELLIPTICAL,
            "EXERCISE_TYPE_BIKING_STATIONARY" to StravaActivityType.RIDE,
        )

        val availableMappings = mappings.mapNotNull { (constantName, expectedType) ->
            constant(constantName)?.let { value -> value to expectedType }
        }

        assumeTrue("No targeted indoor constants found in this Health Connect version", availableMappings.isNotEmpty())

        availableMappings.forEach { (exerciseType, expectedType) ->
            assertEquals(expectedType, mapper.resolve(exerciseType, emptyMap()))
        }
    }

    @Test
    fun `known session types list is sorted and unique`() {
        val known = mapper.knownSessionTypesForSettings()
        assertTrue(known.isNotEmpty())
        assertEquals(known.sorted().distinct(), known)
    }

    private fun constant(name: String): Int? {
        return runCatching {
            ExerciseSessionRecord::class.java.getField(name).getInt(null)
        }.getOrNull()
    }
}
