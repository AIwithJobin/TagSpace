package com.tagspace.localization

import com.tagspace.marker.Marker
import com.tagspace.pose.MachineFrame
import com.tagspace.pose.Pose

/** Where the machine frame sits in this session's AR world, and which marker said so. */
data class MachineFix(val markerId: String, val machineInWorld: Pose)

object Localizer {
    /**
     * Uses the first registered marker that is fully tracked this frame; the list order is the
     * preference order, so the origin marker comes first.
     */
    fun locate(markers: List<Marker>, trackedInWorld: Map<String, Pose>): MachineFix? =
        markers.firstNotNullOfOrNull { marker ->
            val inWorld = trackedInWorld[marker.id] ?: return@firstNotNullOfOrNull null
            val inMachine = marker.poseInMachine ?: return@firstNotNullOfOrNull null
            MachineFix(marker.id, MachineFrame.machineInWorld(inWorld, inMachine))
        }
}
