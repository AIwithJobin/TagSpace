package com.tagspace.ar

import com.tagspace.pose.Pose
import com.tagspace.pose.Quat
import com.tagspace.pose.Vec3

fun com.google.ar.core.Pose.toPose() = Pose(
    Vec3(tx().toDouble(), ty().toDouble(), tz().toDouble()),
    Quat(qx().toDouble(), qy().toDouble(), qz().toDouble(), qw().toDouble()),
)
