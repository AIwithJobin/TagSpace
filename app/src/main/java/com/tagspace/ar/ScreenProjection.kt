package com.tagspace.ar

import androidx.compose.ui.geometry.Offset
import com.tagspace.pose.Vec3

/** Projects world points to screen pixels using ARCore's column-major projection × view matrix. */
class ScreenProjection(private val viewProjection: FloatArray, private val width: Float, private val height: Float) {

    /** Null when the point is behind the camera. */
    fun project(p: Vec3): Offset? {
        val m = viewProjection
        val x = (m[0] * p.x + m[4] * p.y + m[8] * p.z + m[12])
        val y = (m[1] * p.x + m[5] * p.y + m[9] * p.z + m[13])
        val w = (m[3] * p.x + m[7] * p.y + m[11] * p.z + m[15])
        if (w <= 1e-6) return null
        return Offset(((x / w + 1) / 2 * width).toFloat(), ((1 - y / w) / 2 * height).toFloat())
    }
}
