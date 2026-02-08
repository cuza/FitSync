package dev.cuza.FitSync.domain.export

import dev.cuza.FitSync.domain.model.StravaActivityType
import dev.cuza.FitSync.domain.model.WorkoutSample
import dev.cuza.FitSync.domain.model.WorkoutSession
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

class TcxExporterTest {

    private val exporter = TcxExporter()

    @Test
    fun `exported trackpoint timestamps are monotonic`() {
        val session = WorkoutSession(
            healthConnectSessionId = "session-1",
            title = "Treadmill",
            startTime = Instant.parse("2026-01-01T10:00:00Z"),
            endTime = Instant.parse("2026-01-01T10:30:00Z"),
            exerciseType = 1,
            totalDistanceMeters = 5000.0,
            totalCaloriesKcal = 400.0,
            totalSteps = 6000,
            samples = listOf(
                WorkoutSample(
                    timestamp = Instant.parse("2026-01-01T10:10:00Z"),
                    heartRateBpm = 150,
                    distanceMeters = 1600.0,
                ),
                WorkoutSample(
                    timestamp = Instant.parse("2026-01-01T10:05:00Z"),
                    heartRateBpm = 140,
                    distanceMeters = 800.0,
                ),
                WorkoutSample(
                    timestamp = Instant.parse("2026-01-01T10:20:00Z"),
                    heartRateBpm = 155,
                    distanceMeters = 3300.0,
                ),
            ),
        )

        val xml = exporter.buildTcxXml(session, StravaActivityType.RUN)
        val document = parse(xml)
        val trackpoints = document.getElementsByTagName("Trackpoint")

        var previous: Instant? = null
        for (index in 0 until trackpoints.length) {
            val trackpoint = trackpoints.item(index) as Element
            val timeText = trackpoint.getElementsByTagName("Time").item(0).textContent
            val current = Instant.parse(timeText)
            previous?.let { assertTrue("Trackpoints must be monotonic", !current.isBefore(it)) }
            previous = current
        }
    }

    @Test
    fun `export includes heart rate when available and XML is parseable`() {
        val session = WorkoutSession(
            healthConnectSessionId = "session-2",
            title = "Indoor row",
            startTime = Instant.parse("2026-01-01T11:00:00Z"),
            endTime = Instant.parse("2026-01-01T11:20:00Z"),
            exerciseType = 2,
            totalDistanceMeters = null,
            totalCaloriesKcal = 230.0,
            totalSteps = null,
            samples = listOf(
                WorkoutSample(timestamp = Instant.parse("2026-01-01T11:01:00Z"), heartRateBpm = 130),
                WorkoutSample(timestamp = Instant.parse("2026-01-01T11:05:00Z"), heartRateBpm = 142),
            ),
        )

        val xml = exporter.buildTcxXml(session, StravaActivityType.ROWING)
        parse(xml)

        assertTrue(xml.contains("<HeartRateBpm><Value>130</Value></HeartRateBpm>"))
        assertTrue(xml.contains("<HeartRateBpm><Value>142</Value></HeartRateBpm>"))
    }

    private fun parse(xml: String) = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
}
