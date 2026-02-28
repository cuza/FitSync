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
    fun `bike or cycling exercise constants map to ride when available`() {
        val bikeTypeValues = ExerciseSessionRecord::class.java.fields
            .filter { it.name.startsWith("EXERCISE_TYPE_") && it.type == Int::class.javaPrimitiveType }
            .filter { field ->
                field.name.contains("BIKING") ||
                    field.name.contains("CYCLING") ||
                    field.name.contains("SPINNING")
            }
            .mapNotNull { field -> runCatching { field.getInt(null) }.getOrNull() }
            .distinct()

        assumeTrue("No biking/cycling constants found in this Health Connect version", bikeTypeValues.isNotEmpty())

        bikeTypeValues.forEach { value ->
            assertEquals(StravaActivityType.RIDE, mapper.resolve(value, emptyMap()))
        }
    }

    @Test
    fun `generic workout title with spin keywords maps to ride`() {
        val genericType = constant("EXERCISE_TYPE_WORKOUT")
            ?: constant("EXERCISE_TYPE_OTHER_WORKOUT")
            ?: Int.MAX_VALUE

        val resolved = mapper.resolve(
            exerciseType = genericType,
            overrides = emptyMap(),
            sessionTitle = "Indoor Cycling - Peloton",
        )

        assertEquals(StravaActivityType.RIDE, resolved)
    }

    @Test
    fun `title fallback maps virtual row and trail run`() {
        val unknownType = Int.MAX_VALUE

        val virtualRow = mapper.resolve(
            exerciseType = unknownType,
            overrides = emptyMap(),
            sessionTitle = "Zwift Virtual Row",
        )
        val trailRun = mapper.resolve(
            exerciseType = unknownType,
            overrides = emptyMap(),
            sessionTitle = "Morning Trail Run",
        )

        assertEquals(StravaActivityType.VIRTUAL_ROW, virtualRow)
        assertEquals(StravaActivityType.TRAIL_RUN, trailRun)
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
