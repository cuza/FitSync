package dev.cuza.FitSync.domain.sync

import dev.cuza.FitSync.domain.model.WorkoutSession
import java.security.MessageDigest

class SessionHashCalculator {

    fun hash(session: WorkoutSession): String {
        val payload = buildString {
            append(session.healthConnectSessionId)
            append('|')
            append(session.startTime.toEpochMilli())
            append('|')
            append(session.endTime.toEpochMilli())
            append('|')
            append(session.exerciseType)
            append('|')
            append(session.totalDistanceMeters ?: 0.0)
            append('|')
            append(session.totalCaloriesKcal ?: 0.0)
            append('|')
            append(session.totalSteps ?: 0)
            session.samples
                .sortedBy { it.timestamp }
                .forEach { sample ->
                    append('|')
                    append(sample.timestamp.toEpochMilli())
                    append(',')
                    append(sample.heartRateBpm ?: -1)
                    append(',')
                    append(sample.distanceMeters ?: -1.0)
                    append(',')
                    append(sample.speedMps ?: -1.0)
                }
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
