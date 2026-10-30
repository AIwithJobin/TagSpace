package com.tagspace.pose

import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Point or direction in metres. */
@Serializable
data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(x * x + y * y + z * z)

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
    }
}

/** Unit quaternion in xyzw order, the same order ARCore uses. */
@Serializable
data class Quat(val x: Double, val y: Double, val z: Double, val w: Double) {
    operator fun times(q: Quat) = Quat(
        w * q.x + x * q.w + y * q.z - z * q.y,
        w * q.y - x * q.z + y * q.w + z * q.x,
        w * q.z + x * q.y - y * q.x + z * q.w,
        w * q.w - x * q.x - y * q.y - z * q.z,
    )

    fun conjugate() = Quat(-x, -y, -z, w)

    fun normalized(): Quat {
        val n = sqrt(x * x + y * y + z * z + w * w)
        return Quat(x / n, y / n, z / n, w / n)
    }

    fun rotate(v: Vec3): Vec3 {
        val u = Vec3(x, y, z)
        val t = u.cross(v) * 2.0
        return v + t * w + u.cross(t)
    }

    companion object {
        val IDENTITY = Quat(0.0, 0.0, 0.0, 1.0)

        fun axisAngle(axis: Vec3, radians: Double): Quat {
            val a = axis * (1.0 / axis.length())
            val s = sin(radians / 2)
            return Quat(a.x * s, a.y * s, a.z * s, cos(radians / 2))
        }
    }
}

/**
 * Rigid transform. Named `T_a_b` in the docs: it maps points expressed in frame b into frame a,
 * so `T_a_b * T_b_c = T_a_c`.
 */
@Serializable
data class Pose(val position: Vec3, val rotation: Quat) {
    fun transform(p: Vec3) = rotation.rotate(p) + position

    operator fun times(o: Pose) = Pose(transform(o.position), (rotation * o.rotation).normalized())

    fun inverse(): Pose {
        val inv = rotation.conjugate()
        return Pose(inv.rotate(position) * -1.0, inv)
    }

    companion object {
        val IDENTITY = Pose(Vec3.ZERO, Quat.IDENTITY)
    }
}
