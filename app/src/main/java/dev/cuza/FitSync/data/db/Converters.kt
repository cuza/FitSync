package dev.cuza.FitSync.data.db

import androidx.room.TypeConverter
import java.time.Instant

class Converters {

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun uploadStatusToString(value: UploadStatus?): String? = value?.name

    @TypeConverter
    fun stringToUploadStatus(value: String?): UploadStatus? = value?.let(UploadStatus::valueOf)
}
