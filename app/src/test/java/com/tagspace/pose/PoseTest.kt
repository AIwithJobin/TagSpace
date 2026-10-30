package com.tagspace.pose

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class PoseTest {

    private val eps = 1e-9

    private fun assertVec(expected: Vec3, actual: Vec3) {
        assertEquals("x", expected.x, actual.x, eps)
        assertEquals("y", expected.y, actual.y, eps)
        assertEquals("z", expected.z, actual.z, eps)
    }

    /** q and -q are the same rotation. */
    private fun assertPose(expected: Pose, actual: Pose) {
        assertVec(expected.position, actual.position)
        val e = expected.rotation
        val a = actual.rotation
        val dot = e.x * a.x + e.y * a.y + e.z * a.z + e.w * a.w
        assertEquals("rotation", 1.0, abs(dot), eps)
    }

    private val wallMarker = Pose(Vec3(1.0, 1.5, -2.0), Quat.axisAngle(Vec3(1.0, 0.0, 0.0), PI / 2))
    private val otherSession = Pose(Vec3(-3.0, 0.2, 4.0), Quat.axisAngle(Vec3(0.3, 1.0, -0.2), 2.1))

    @Test
    fun `rotating 90 degrees about z maps x onto y`() {
        val q = Quat.axisAngle(Vec3(0.0, 0.0, 1.0), PI / 2)
        assertVec(Vec3(0.0, 1.0, 0.0), q.rotate(Vec3(1.0, 0.0, 0.0)))
    }

    @Test
    fun `pose times its inverse is identity`() {
        assertPose(Pose.IDENTITY, otherSession * otherSession.inverse())
        assertPose(Pose.IDENTITY, otherSession.inverse() * otherSession)
    }

    @Test
    fun `composition matches transforming twice`() {
        val p = Vec3(0.3, -0.7, 1.1)
        assertVec(wallMarker.transform(otherSession.transform(p)), (wallMarker * otherSession).transform(p))
    }

    @Test
    fun `point round trips through the machine frame`() {
        val machineInWorld = MachineFrame.machineInWorld(wallMarker, Pose.IDENTITY)
        val p = Vec3(0.4, 0.9, -1.3)
        assertVec(p, MachineFrame.toWorld(machineInWorld, MachineFrame.toMachine(machineInWorld, p)))
    }

    @Test
    fun `part mapped in one session lands on the same physical spot in another`() {
        // The physical room is fixed; each session only sees it through a different world origin.
        val partInRoom = Vec3(0.5, -0.2, 0.8)
        val markerInRoom = Pose(Vec3(2.0, 1.0, 0.0), Quat.axisAngle(Vec3(0.0, 1.0, 0.0), 0.7))
        val roomInWorld1 = Pose.IDENTITY
        val roomInWorld2 = otherSession

        val machine1 = MachineFrame.machineInWorld(roomInWorld1 * markerInRoom, Pose.IDENTITY)
        val stored = MachineFrame.toMachine(machine1, roomInWorld1.transform(partInRoom))

        val machine2 = MachineFrame.machineInWorld(roomInWorld2 * markerInRoom, Pose.IDENTITY)
        assertVec(roomInWorld2.transform(partInRoom), MachineFrame.toWorld(machine2, stored))
    }

    @Test
    fun `second marker localises to the same machine frame as the origin`() {
        val origin = Pose.IDENTITY
        val markerBInWorld = Pose(Vec3(-1.0, 1.2, 3.0), Quat.axisAngle(Vec3(0.0, 1.0, 0.0), PI))

        val machineInWorld = MachineFrame.machineInWorld(wallMarker, origin)
        val bInMachine = MachineFrame.markerInMachine(machineInWorld, markerBInWorld)

        // New session: only marker B is visible.
        val bInWorld2 = otherSession * markerBInWorld
        val fromB = MachineFrame.machineInWorld(bInWorld2, bInMachine)
        val fromOrigin = MachineFrame.machineInWorld(otherSession * wallMarker, origin)
        assertPose(fromOrigin, fromB)
    }
}
