package com.tagspace.pose

/**
 * The marker-anchored machine coordinate frame. Parts and markers are stored in it, so they
 * survive AR sessions whose world origin is different every time.
 */
object MachineFrame {
    /** Localise: T_world_machine = T_world_M · inverse(T_machine_M). */
    fun machineInWorld(markerInWorld: Pose, markerInMachine: Pose): Pose =
        markerInWorld * markerInMachine.inverse()

    /** Map a part: p_machine = inverse(T_world_machine) · p_world. */
    fun toMachine(machineInWorld: Pose, pointInWorld: Vec3): Vec3 =
        machineInWorld.inverse().transform(pointInWorld)

    /** Render a part: p_world = T_world_machine · p_machine. */
    fun toWorld(machineInWorld: Pose, pointInMachine: Vec3): Vec3 =
        machineInWorld.transform(pointInMachine)

    /** Register a marker: T_machine_B = inverse(T_world_machine) · T_world_B. */
    fun markerInMachine(machineInWorld: Pose, markerInWorld: Pose): Pose =
        machineInWorld.inverse() * markerInWorld
}
