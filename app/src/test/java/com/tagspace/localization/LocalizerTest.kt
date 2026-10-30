package com.tagspace.localization

import com.tagspace.marker.Marker
import com.tagspace.pose.Pose
import com.tagspace.pose.Quat
import com.tagspace.pose.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalizerTest {

    private val origin = Marker("a", 0.15, Pose.IDENTITY, isOrigin = true)
    private val registered = Marker("b", 0.15, Pose(Vec3(2.0, 0.0, 0.0), Quat.IDENTITY))
    private val unregistered = Marker("c", 0.15)
    private val markers = listOf(origin, registered, unregistered)

    private val seen = Pose(Vec3(1.0, 1.0, 1.0), Quat.IDENTITY)

    @Test
    fun `no fix when nothing is tracked`() {
        assertNull(Localizer.locate(markers, emptyMap()))
    }

    @Test
    fun `unregistered marker gives no fix`() {
        assertNull(Localizer.locate(markers, mapOf("c" to seen)))
    }

    @Test
    fun `origin marker wins when several are tracked`() {
        assertEquals("a", Localizer.locate(markers, mapOf("b" to seen, "a" to seen))?.markerId)
    }

    @Test
    fun `registered marker places the machine frame by its stored offset`() {
        val fix = Localizer.locate(markers, mapOf("b" to seen))!!
        assertEquals(Vec3(-1.0, 1.0, 1.0), fix.machineInWorld.position)
    }
}
