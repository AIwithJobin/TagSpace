package com.tagspace.ar

import androidx.compose.ui.geometry.Offset
import com.tagspace.pose.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenProjectionTest {

    // Column-major OpenGL-style perspective: w = -z, so points in front of the camera have z < 0.
    private val perspective = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, -1f, -1f,
        0f, 0f, 0f, 0f,
    )
    private val projection = ScreenProjection(perspective, 1000f, 2000f)

    @Test
    fun `point straight ahead lands in the screen centre`() {
        assertEquals(Offset(500f, 1000f), projection.project(Vec3(0.0, 0.0, -2.0)))
    }

    @Test
    fun `up and right in the world is up and right on screen`() {
        assertEquals(Offset(1000f, 0f), projection.project(Vec3(1.0, 1.0, -1.0)))
    }

    @Test
    fun `point behind the camera is not drawn`() {
        assertNull(projection.project(Vec3(0.0, 0.0, 1.0)))
    }
}
