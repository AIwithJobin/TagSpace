package com.tagspace.marker

import com.tagspace.pose.Pose

/** Printed width; must match MARKER_WIDTH_CM in tools/generate_markers.py. */
const val MARKER_WIDTH_M = 0.15

// shortcut: fixed room setup with marker-02 unregistered, replace with stored markers when mapping registers them (phase A3).
val DEMO_MARKERS = listOf(
    Marker("marker-01", MARKER_WIDTH_M, poseInMachine = Pose.IDENTITY, isOrigin = true),
    Marker("marker-02", MARKER_WIDTH_M),
)
