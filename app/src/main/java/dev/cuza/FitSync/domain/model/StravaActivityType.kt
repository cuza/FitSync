package dev.cuza.FitSync.domain.model

enum class StravaActivityType(
    // Canonical Strava sport_type value.
    val uploadValue: String,
    val displayName: String,
    val tcxSport: String,
    // activity_type fallback used for older/limited upload handling.
    val activityTypeValue: String,
) {
    WORKOUT(uploadValue = "Workout", displayName = "Workout", tcxSport = "Other", activityTypeValue = "workout"),
    RUN(uploadValue = "Run", displayName = "Run", tcxSport = "Running", activityTypeValue = "run"),
    RIDE(uploadValue = "Ride", displayName = "Ride", tcxSport = "Biking", activityTypeValue = "ride"),
    WALK(uploadValue = "Walk", displayName = "Walk", tcxSport = "Walking", activityTypeValue = "walk"),
    HIKE(uploadValue = "Hike", displayName = "Hike", tcxSport = "Hiking", activityTypeValue = "hike"),
    SWIM(uploadValue = "Swim", displayName = "Swim", tcxSport = "Swimming", activityTypeValue = "swim"),
    ROWING(uploadValue = "Rowing", displayName = "Rowing", tcxSport = "Other", activityTypeValue = "rowing"),
    ELLIPTICAL(uploadValue = "Elliptical", displayName = "Elliptical", tcxSport = "Other", activityTypeValue = "elliptical"),
    STAIR_STEPPER(uploadValue = "StairStepper", displayName = "Stair Stepper", tcxSport = "Other", activityTypeValue = "stairstepper"),
    WEIGHT_TRAINING(uploadValue = "WeightTraining", displayName = "Weight Training", tcxSport = "Other", activityTypeValue = "weighttraining"),
    YOGA(uploadValue = "Yoga", displayName = "Yoga", tcxSport = "Other", activityTypeValue = "yoga"),
    PILATES(uploadValue = "Pilates", displayName = "Pilates", tcxSport = "Other", activityTypeValue = "workout"),
    HIGH_INTENSITY_INTERVAL_TRAINING(
        uploadValue = "HighIntensityIntervalTraining",
        displayName = "HIIT",
        tcxSport = "Other",
        activityTypeValue = "workout",
    ),
    CROSS_FIT(uploadValue = "Crossfit", displayName = "Crossfit", tcxSport = "Other", activityTypeValue = "crossfit"),
    TRAIL_RUN(uploadValue = "TrailRun", displayName = "Trail Run", tcxSport = "Running", activityTypeValue = "run"),
    MOUNTAIN_BIKE_RIDE(
        uploadValue = "MountainBikeRide",
        displayName = "Mountain Bike Ride",
        tcxSport = "Biking",
        activityTypeValue = "ride",
    ),
    GRAVEL_RIDE(uploadValue = "GravelRide", displayName = "Gravel Ride", tcxSport = "Biking", activityTypeValue = "ride"),
    E_BIKE_RIDE(uploadValue = "EBikeRide", displayName = "E-Bike Ride", tcxSport = "Biking", activityTypeValue = "ebikeride"),
    E_MOUNTAIN_BIKE_RIDE(
        uploadValue = "EMountainBikeRide",
        displayName = "E-Mountain Bike Ride",
        tcxSport = "Biking",
        activityTypeValue = "ebikeride",
    ),
    VIRTUAL_RIDE(uploadValue = "VirtualRide", displayName = "Virtual Ride", tcxSport = "Biking", activityTypeValue = "virtualride"),
    VIRTUAL_RUN(uploadValue = "VirtualRun", displayName = "Virtual Run", tcxSport = "Running", activityTypeValue = "virtualrun"),
    VIRTUAL_ROW(uploadValue = "VirtualRow", displayName = "Virtual Row", tcxSport = "Other", activityTypeValue = "rowing"),
    ALPINE_SKI(uploadValue = "AlpineSki", displayName = "Alpine Ski", tcxSport = "Other", activityTypeValue = "alpineski"),
    BACKCOUNTRY_SKI(
        uploadValue = "BackcountrySki",
        displayName = "Backcountry Ski",
        tcxSport = "Other",
        activityTypeValue = "backcountryski",
    ),
    NORDIC_SKI(uploadValue = "NordicSki", displayName = "Nordic Ski", tcxSport = "Other", activityTypeValue = "nordicski"),
    ROLLER_SKI(uploadValue = "RollerSki", displayName = "Roller Ski", tcxSport = "Other", activityTypeValue = "rollerski"),
    SNOWBOARD(uploadValue = "Snowboard", displayName = "Snowboard", tcxSport = "Other", activityTypeValue = "snowboard"),
    SNOWSHOE(uploadValue = "Snowshoe", displayName = "Snowshoe", tcxSport = "Other", activityTypeValue = "snowshoe"),
    ICE_SKATE(uploadValue = "IceSkate", displayName = "Ice Skate", tcxSport = "Other", activityTypeValue = "iceskate"),
    INLINE_SKATE(uploadValue = "InlineSkate", displayName = "Inline Skate", tcxSport = "Other", activityTypeValue = "inlineskate"),
    SKATEBOARD(uploadValue = "Skateboard", displayName = "Skateboard", tcxSport = "Other", activityTypeValue = "skateboard"),
    GOLF(uploadValue = "Golf", displayName = "Golf", tcxSport = "Other", activityTypeValue = "golf"),
    SOCCER(uploadValue = "Soccer", displayName = "Soccer", tcxSport = "Other", activityTypeValue = "soccer"),
    TENNIS(uploadValue = "Tennis", displayName = "Tennis", tcxSport = "Other", activityTypeValue = "workout"),
    TABLE_TENNIS(uploadValue = "TableTennis", displayName = "Table Tennis", tcxSport = "Other", activityTypeValue = "workout"),
    BADMINTON(uploadValue = "Badminton", displayName = "Badminton", tcxSport = "Other", activityTypeValue = "workout"),
    PICKLEBALL(uploadValue = "Pickleball", displayName = "Pickleball", tcxSport = "Other", activityTypeValue = "workout"),
    SQUASH(uploadValue = "Squash", displayName = "Squash", tcxSport = "Other", activityTypeValue = "workout"),
    RACQUETBALL(uploadValue = "Racquetball", displayName = "Racquetball", tcxSport = "Other", activityTypeValue = "workout"),
    ROCK_CLIMBING(uploadValue = "RockClimbing", displayName = "Rock Climbing", tcxSport = "Other", activityTypeValue = "rockclimbing"),
    CANOEING(uploadValue = "Canoeing", displayName = "Canoeing", tcxSport = "Other", activityTypeValue = "canoeing"),
    KAYAKING(uploadValue = "Kayaking", displayName = "Kayaking", tcxSport = "Other", activityTypeValue = "kayaking"),
    STAND_UP_PADDLING(
        uploadValue = "StandUpPaddling",
        displayName = "Stand Up Paddling",
        tcxSport = "Other",
        activityTypeValue = "standuppaddling",
    ),
    SURFING(uploadValue = "Surfing", displayName = "Surfing", tcxSport = "Other", activityTypeValue = "surfing"),
    WINDSURF(uploadValue = "Windsurf", displayName = "Windsurf", tcxSport = "Other", activityTypeValue = "windsurf"),
    KITESURF(uploadValue = "Kitesurf", displayName = "Kitesurf", tcxSport = "Other", activityTypeValue = "kitesurf"),
    SAIL(uploadValue = "Sail", displayName = "Sail", tcxSport = "Other", activityTypeValue = "sail"),
    HANDCYCLE(uploadValue = "Handcycle", displayName = "Handcycle", tcxSport = "Other", activityTypeValue = "handcycle"),
    WHEELCHAIR(uploadValue = "Wheelchair", displayName = "Wheelchair", tcxSport = "Other", activityTypeValue = "wheelchair"),
    VELOMOBILE(uploadValue = "Velomobile", displayName = "Velomobile", tcxSport = "Other", activityTypeValue = "velomobile");

    companion object {
        fun fromUploadValue(value: String?): StravaActivityType {
            val normalized = value?.trim()?.lowercase() ?: return WORKOUT
            return LOOKUP[normalized] ?: WORKOUT
        }

        private val LOOKUP: Map<String, StravaActivityType> = buildMap {
            StravaActivityType.entries.forEach { type ->
                put(type.uploadValue.lowercase(), type)
                putIfAbsent(type.activityTypeValue.lowercase(), type)
                put(type.name.lowercase(), type)
            }
            // Backward compatibility for older stored values and aliases.
            put("mountainbikeride", MOUNTAIN_BIKE_RIDE)
            put("mtb", MOUNTAIN_BIKE_RIDE)
            put("ebike", E_BIKE_RIDE)
            put("emountainbike", E_MOUNTAIN_BIKE_RIDE)
            put("virtualrowing", VIRTUAL_ROW)
            put("hiit", HIGH_INTENSITY_INTERVAL_TRAINING)
            put("strength", WEIGHT_TRAINING)
        }
    }
}
