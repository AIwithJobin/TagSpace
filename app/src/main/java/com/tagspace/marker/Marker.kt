package com.tagspace.marker

import com.tagspace.pose.Pose
import kotlinx.serialization.Serializable

@Serializable
data class Marker(
    /** Also the image asset name (`assets/markers/<id>.png`) and the ARCore image name. */
    val id: String,
    val physicalWidthM: Double,
    /** T_machine_marker; null until the marker has been registered. */
    val poseInMachine: Pose? = null,
    val isOrigin: Boolean = false,
)
