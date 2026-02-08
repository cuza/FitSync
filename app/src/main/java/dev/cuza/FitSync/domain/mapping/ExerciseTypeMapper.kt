package dev.cuza.FitSync.domain.mapping

import androidx.health.connect.client.records.ExerciseSessionRecord
import dev.cuza.FitSync.domain.model.StravaActivityType

class ExerciseTypeMapper {

    private val rowingTypes = setOfNotNull(
        constantValue("EXERCISE_TYPE_ROWING"),
        constantValue("EXERCISE_TYPE_ROWING_MACHINE"),
        constantValue("EXERCISE_TYPE_ERGOMETER"),
    )

    private val treadmillTypes = setOfNotNull(
        constantValue("EXERCISE_TYPE_RUNNING_TREADMILL"),
        constantValue("EXERCISE_TYPE_TREADMILL"),
    )

    private val ellipticalTypes = setOfNotNull(
        constantValue("EXERCISE_TYPE_ELLIPTICAL"),
    )

    private val stationaryBikeTypes = setOfNotNull(
        constantValue("EXERCISE_TYPE_BIKING_STATIONARY"),
        constantValue("EXERCISE_TYPE_SPINNING"),
    )

    fun resolve(exerciseType: Int, overrides: Map<Int, StravaActivityType>): StravaActivityType {
        overrides[exerciseType]?.let { return it }

        return when {
            exerciseType in rowingTypes -> StravaActivityType.ROWING
            exerciseType in treadmillTypes -> StravaActivityType.RUN
            exerciseType in ellipticalTypes -> StravaActivityType.ELLIPTICAL
            exerciseType in stationaryBikeTypes -> StravaActivityType.RIDE
            else -> StravaActivityType.WORKOUT
        }
    }

    fun knownSessionTypesForSettings(): List<Int> {
        val fromApi = ExerciseSessionRecord::class.java.fields
            .filter { it.name.startsWith("EXERCISE_TYPE_") && it.type == Int::class.javaPrimitiveType }
            .mapNotNull { field ->
                runCatching { field.getInt(null) }.getOrNull()
            }
            .distinct()
        return fromApi.sorted()
    }

    fun displayName(exerciseType: Int): String {
        return ExerciseSessionRecord::class.java.fields
            .firstOrNull { field ->
                field.name.startsWith("EXERCISE_TYPE_") &&
                    field.type == Int::class.javaPrimitiveType &&
                    runCatching { field.getInt(null) == exerciseType }.getOrDefault(false)
            }
            ?.name
            ?.removePrefix("EXERCISE_TYPE_")
            ?.replace('_', ' ')
            ?.lowercase()
            ?.replaceFirstChar { it.uppercase() }
            ?: "Type $exerciseType"
    }

    private fun constantValue(name: String): Int? {
        return runCatching {
            ExerciseSessionRecord::class.java.getField(name).getInt(null)
        }.getOrNull()
    }
}
