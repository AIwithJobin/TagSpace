package com.tagspace.ar

import android.opengl.Matrix
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.tagspace.localization.Localizer
import com.tagspace.localization.MachineFix
import com.tagspace.marker.Marker

/** Per-frame AR state the overlay draws from. */
class ArState(private val markers: List<Marker>) {
    /** Last known fix; kept when the marker leaves view because ARCore keeps tracking the world. */
    var fix by mutableStateOf<MachineFix?>(null)
        private set
    /** Marker giving a fix in the current frame, null when none is in view. */
    var liveMarkerId by mutableStateOf<String?>(null)
        private set
    var viewProjection by mutableStateOf<FloatArray?>(null)
        private set
    var trackingFailure by mutableStateOf(TrackingFailureReason.NONE)

    fun onFrame(session: Session, frame: Frame) {
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            viewProjection = null
            return
        }
        val tracked = session.getAllTrackables(AugmentedImage::class.java)
            .filter { it.trackingState == TrackingState.TRACKING && it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING }
            .associate { it.name to it.centerPose.toPose() }
        val live = Localizer.locate(markers, tracked)
        if (live != null) fix = live
        liveMarkerId = live?.markerId

        val view = FloatArray(16)
        val projection = FloatArray(16)
        camera.getViewMatrix(view, 0)
        camera.getProjectionMatrix(projection, 0, 0.05f, 100f)
        viewProjection = FloatArray(16).also { Matrix.multiplyMM(it, 0, projection, 0, view, 0) }
    }
}
