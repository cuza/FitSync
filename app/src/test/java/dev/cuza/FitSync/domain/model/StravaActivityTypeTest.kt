package dev.cuza.FitSync.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StravaActivityTypeTest {

    @Test
    fun `fromUploadValue supports canonical sport_type values`() {
        assertEquals(StravaActivityType.VIRTUAL_RUN, StravaActivityType.fromUploadValue("VirtualRun"))
        assertEquals(StravaActivityType.GRAVEL_RIDE, StravaActivityType.fromUploadValue("GravelRide"))
    }

    @Test
    fun `fromUploadValue stays backward compatible with old lowercase values`() {
        assertEquals(StravaActivityType.RIDE, StravaActivityType.fromUploadValue("ride"))
        assertEquals(StravaActivityType.WORKOUT, StravaActivityType.fromUploadValue("workout"))
    }
}
