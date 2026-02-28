package dev.cuza.FitSync.domain.mapping

import androidx.health.connect.client.records.ExerciseSessionRecord
import dev.cuza.FitSync.domain.model.StravaActivityType

class ExerciseTypeMapper {

    private val typeNameByValue: Map<Int, String> = ExerciseSessionRecord::class.java.fields
        .asSequence()
        .filter { it.name.startsWith("EXERCISE_TYPE_") && it.type == Int::class.javaPrimitiveType }
        .mapNotNull { field ->
            runCatching { field.getInt(null) to field.name.removePrefix("EXERCISE_TYPE_") }.getOrNull()
        }
        .toMap()

    private val knownSessionTypes: Set<Int> = typeNameByValue.keys

    fun resolve(
        exerciseType: Int,
        overrides: Map<Int, StravaActivityType>,
        sessionTitle: String? = null,
    ): StravaActivityType {
        overrides[exerciseType]?.let { return it }

        val typeName = typeNameByValue[exerciseType]
        resolveByTypeName(typeName)?.let { return it }
        resolveByTitle(sessionTitle)?.let { return it }
        return StravaActivityType.WORKOUT
    }

    fun knownSessionTypesForSettings(): List<Int> = knownSessionTypes.sorted()

    fun displayName(exerciseType: Int): String {
        return typeNameByValue[exerciseType]
            ?.replace('_', ' ')
            ?.lowercase()
            ?.replaceFirstChar { it.uppercase() }
            ?: "Type $exerciseType"
    }

    private fun resolveByTypeName(typeName: String?): StravaActivityType? {
        val n = typeName?.uppercase() ?: return null

        return when {
            n.contains("VIRTUAL") && hasAny(n, "ROW", "ROWING", "ERG") -> StravaActivityType.VIRTUAL_ROW
            n.contains("VIRTUAL") && hasAny(n, "RUN", "JOG") -> StravaActivityType.VIRTUAL_RUN
            n.contains("VIRTUAL") && hasAny(n, "BIKE", "BIKING", "CYCL") -> StravaActivityType.VIRTUAL_RIDE

            hasAny(n, "ROWING", "ROWER", "ERGOMETER") -> StravaActivityType.ROWING
            hasAny(n, "ELLIPTICAL") -> StravaActivityType.ELLIPTICAL
            hasAny(n, "STAIR", "STEPPER") -> StravaActivityType.STAIR_STEPPER

            n.contains("TRAIL") && hasAny(n, "RUN", "RUNNING", "JOG") -> StravaActivityType.TRAIL_RUN
            hasAny(n, "RUN", "RUNNING", "JOG", "TREADMILL") -> StravaActivityType.RUN
            hasAny(n, "WALK") -> StravaActivityType.WALK
            hasAny(n, "HIKE", "HIKING", "TREK") -> StravaActivityType.HIKE
            hasAny(n, "SWIM", "SWIMMING") -> StravaActivityType.SWIM

            hasAny(n, "E_MOUNTAIN_BIKE", "E_MTB") -> StravaActivityType.E_MOUNTAIN_BIKE_RIDE
            hasAny(n, "E_BIKE", "EBIKE", "ELECTRIC_BIKE") && hasAny(n, "MOUNTAIN", "MTB") ->
                StravaActivityType.E_MOUNTAIN_BIKE_RIDE
            hasAny(n, "E_BIKE", "EBIKE", "ELECTRIC_BIKE") -> StravaActivityType.E_BIKE_RIDE
            n.contains("MOUNTAIN") && hasAny(n, "BIKE", "BIKING", "CYCL") -> StravaActivityType.MOUNTAIN_BIKE_RIDE
            n.contains("GRAVEL") && hasAny(n, "BIKE", "BIKING", "CYCL") -> StravaActivityType.GRAVEL_RIDE
            hasAny(n, "SPINNING", "SPIN", "BIKE", "BIKING", "CYCL", "STATIONARY_BIKE") -> StravaActivityType.RIDE

            hasAny(n, "HIIT", "HIGH_INTENSITY_INTERVAL_TRAINING") ->
                StravaActivityType.HIGH_INTENSITY_INTERVAL_TRAINING
            hasAny(n, "CROSSFIT", "CROSS_FIT") -> StravaActivityType.CROSS_FIT
            hasAny(n, "WEIGHT", "STRENGTH", "RESISTANCE_TRAINING", "WEIGHTLIFTING") ->
                StravaActivityType.WEIGHT_TRAINING
            hasAny(n, "YOGA") -> StravaActivityType.YOGA
            hasAny(n, "PILATES") -> StravaActivityType.PILATES

            hasAny(n, "BADMINTON") -> StravaActivityType.BADMINTON
            hasAny(n, "PICKLEBALL") -> StravaActivityType.PICKLEBALL
            hasAny(n, "RACQUETBALL") -> StravaActivityType.RACQUETBALL
            hasAny(n, "SQUASH") -> StravaActivityType.SQUASH
            hasAny(n, "TABLE_TENNIS", "PING_PONG") -> StravaActivityType.TABLE_TENNIS
            hasAny(n, "TENNIS") -> StravaActivityType.TENNIS
            hasAny(n, "SOCCER", "FOOTBALL_SOCCER") -> StravaActivityType.SOCCER

            hasAny(n, "ROCK_CLIMBING", "CLIMBING") -> StravaActivityType.ROCK_CLIMBING
            hasAny(n, "GOLF") -> StravaActivityType.GOLF

            hasAny(n, "CANOE") -> StravaActivityType.CANOEING
            hasAny(n, "KAYAK") -> StravaActivityType.KAYAKING
            hasAny(n, "STAND_UP_PADDLING", "SUP") -> StravaActivityType.STAND_UP_PADDLING
            hasAny(n, "KITESURF") -> StravaActivityType.KITESURF
            hasAny(n, "WINDSURF") -> StravaActivityType.WINDSURF
            hasAny(n, "SURF") -> StravaActivityType.SURFING
            hasAny(n, "SAIL") -> StravaActivityType.SAIL

            hasAny(n, "SNOWBOARD") -> StravaActivityType.SNOWBOARD
            hasAny(n, "SNOWSHOE") -> StravaActivityType.SNOWSHOE
            hasAny(n, "ALPINE_SKI") -> StravaActivityType.ALPINE_SKI
            hasAny(n, "BACKCOUNTRY_SKI") -> StravaActivityType.BACKCOUNTRY_SKI
            hasAny(n, "NORDIC_SKI", "CROSS_COUNTRY_SKI") -> StravaActivityType.NORDIC_SKI
            hasAny(n, "ROLLER_SKI") -> StravaActivityType.ROLLER_SKI
            hasAny(n, "ICE_SKATE") -> StravaActivityType.ICE_SKATE
            hasAny(n, "INLINE_SKATE") -> StravaActivityType.INLINE_SKATE
            hasAny(n, "SKATEBOARD") -> StravaActivityType.SKATEBOARD

            hasAny(n, "HANDCYCLE", "HAND_CYCLE") -> StravaActivityType.HANDCYCLE
            hasAny(n, "WHEELCHAIR") -> StravaActivityType.WHEELCHAIR
            hasAny(n, "VELOMOBILE") -> StravaActivityType.VELOMOBILE

            else -> null
        }
    }

    private fun resolveByTitle(sessionTitle: String?): StravaActivityType? {
        if (sessionTitle.isNullOrBlank()) return null
        val t = sessionTitle.lowercase()

        val isVirtual = hasAny(t, "virtual", "zwift", "rouvy", "mywhoosh")
        if (isVirtual && hasAny(t, "row", "rowing", "erg")) return StravaActivityType.VIRTUAL_ROW
        if (isVirtual && hasAny(t, "run", "treadmill")) return StravaActivityType.VIRTUAL_RUN
        if (isVirtual && hasAny(t, "ride", "bike", "cycling", "spin", "trainer", "peloton")) {
            return StravaActivityType.VIRTUAL_RIDE
        }

        return when {
            hasAny(t, "trail run") -> StravaActivityType.TRAIL_RUN
            hasAny(t, "spin", "spinning", "indoor cycling", "peloton", "bike", "cycling") -> StravaActivityType.RIDE
            hasAny(t, "row", "rowing", "erg") -> StravaActivityType.ROWING
            hasAny(t, "walk") -> StravaActivityType.WALK
            hasAny(t, "hike", "hiking") -> StravaActivityType.HIKE
            hasAny(t, "run", "jog", "treadmill") -> StravaActivityType.RUN
            hasAny(t, "swim") -> StravaActivityType.SWIM
            hasAny(t, "hiit") -> StravaActivityType.HIGH_INTENSITY_INTERVAL_TRAINING
            hasAny(t, "crossfit") -> StravaActivityType.CROSS_FIT
            hasAny(t, "weights", "weight training", "strength") -> StravaActivityType.WEIGHT_TRAINING
            hasAny(t, "yoga") -> StravaActivityType.YOGA
            hasAny(t, "pilates") -> StravaActivityType.PILATES
            else -> null
        }
    }

    private fun hasAny(text: String, vararg tokens: String): Boolean {
        return tokens.any { token -> text.contains(token, ignoreCase = true) }
    }
}
