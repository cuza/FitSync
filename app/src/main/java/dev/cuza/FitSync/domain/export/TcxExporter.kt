package dev.cuza.FitSync.domain.export

import dev.cuza.FitSync.domain.model.StravaActivityType
import dev.cuza.FitSync.domain.model.WorkoutSample
import dev.cuza.FitSync.domain.model.WorkoutSession
import java.io.File
import java.time.Duration
import java.time.format.DateTimeFormatter

class TcxExporter {

    fun export(
        session: WorkoutSession,
        activityType: StravaActivityType,
        outputDir: File,
    ): File {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val samples = sanitizeSamples(session)
        val file = File(outputDir, "${session.healthConnectSessionId}.tcx")
        file.writeText(buildTcxXml(session, activityType, samples))
        return file
    }

    fun buildTcxXml(
        session: WorkoutSession,
        activityType: StravaActivityType,
        samples: List<WorkoutSample> = sanitizeSamples(session),
    ): String {
        val startIso = iso(session.startTime)
        val totalSeconds = Duration.between(session.startTime, session.endTime).seconds.coerceAtLeast(0)
        val totalDistance = session.totalDistanceMeters ?: samples.lastOrNull()?.distanceMeters ?: 0.0
        val calories = (session.totalCaloriesKcal ?: 0.0).toInt().coerceAtLeast(0)

        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<TrainingCenterDatabase xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\"")
            appendLine("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
            appendLine("    xmlns:ns3=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2\"")
            appendLine("    xsi:schemaLocation=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2 https://www8.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd\">")
            appendLine("  <Activities>")
            appendLine("    <Activity Sport=\"${activityType.tcxSport}\">")
            appendLine("      <Id>$startIso</Id>")
            appendLine("      <Lap StartTime=\"$startIso\">")
            appendLine("        <TotalTimeSeconds>$totalSeconds</TotalTimeSeconds>")
            appendLine("        <DistanceMeters>${formatDouble(totalDistance)}</DistanceMeters>")
            appendLine("        <Calories>$calories</Calories>")
            appendLine("        <Intensity>Active</Intensity>")
            appendLine("        <TriggerMethod>Manual</TriggerMethod>")
            appendLine("        <Track>")

            samples.forEach { sample ->
                appendLine("          <Trackpoint>")
                appendLine("            <Time>${iso(sample.timestamp)}</Time>")
                sample.distanceMeters?.let { distance ->
                    appendLine("            <DistanceMeters>${formatDouble(distance)}</DistanceMeters>")
                }
                sample.heartRateBpm?.let { bpm ->
                    appendLine("            <HeartRateBpm><Value>$bpm</Value></HeartRateBpm>")
                }
                sample.speedMps?.let { speed ->
                    appendLine("            <Extensions>")
                    appendLine("              <ns3:TPX>")
                    appendLine("                <ns3:Speed>${formatDouble(speed)}</ns3:Speed>")
                    appendLine("              </ns3:TPX>")
                    appendLine("            </Extensions>")
                }
                appendLine("          </Trackpoint>")
            }

            appendLine("        </Track>")
            appendLine("      </Lap>")
            appendLine("      <Notes>${escapeXml(session.title)}</Notes>")
            appendLine("    </Activity>")
            appendLine("  </Activities>")
            appendLine("</TrainingCenterDatabase>")
        }
    }

    private fun sanitizeSamples(session: WorkoutSession): List<WorkoutSample> {
        val sorted = session.samples
            .distinctBy { it.timestamp }
            .sortedBy { it.timestamp }

        if (sorted.isNotEmpty()) {
            return sorted
        }

        return listOf(
            WorkoutSample(timestamp = session.startTime),
            WorkoutSample(timestamp = session.endTime),
        ).distinctBy { it.timestamp }
    }

    private fun iso(instant: java.time.Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)

    private fun formatDouble(value: Double): String = "%.2f".format(java.util.Locale.US, value)

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
