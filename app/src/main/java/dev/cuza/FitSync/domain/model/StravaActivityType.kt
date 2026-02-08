package dev.cuza.FitSync.domain.model

enum class StravaActivityType(
    val uploadValue: String,
    val displayName: String,
    val tcxSport: String,
) {
    ROWING(uploadValue = "rowing", displayName = "Rowing", tcxSport = "Other"),
    RUN(uploadValue = "run", displayName = "Run", tcxSport = "Running"),
    ELLIPTICAL(uploadValue = "elliptical", displayName = "Elliptical", tcxSport = "Other"),
    RIDE(uploadValue = "ride", displayName = "Ride", tcxSport = "Biking"),
    WORKOUT(uploadValue = "workout", displayName = "Workout", tcxSport = "Other");

    companion object {
        fun fromUploadValue(value: String?): StravaActivityType {
            return entries.firstOrNull { it.uploadValue == value } ?: WORKOUT
        }
    }
}
