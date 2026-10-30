package com.tagspace.ar

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.TrackingFailureReason
import com.tagspace.marker.Marker
import com.tagspace.pose.MachineFrame
import com.tagspace.pose.Vec3
import io.github.sceneview.ar.ARScene

private const val AXIS_LENGTH_M = 0.1

@Composable
fun ArScreen(markers: List<Marker>) {
    val context = LocalContext.current
    val state = remember { ArState(markers) }
    val images = remember {
        markers.associate { m -> m.id to context.assets.open("markers/${m.id}.png").use(BitmapFactory::decodeStream) }
    }

    Box(Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            sessionConfiguration = { session, config ->
                config.focusMode = Config.FocusMode.AUTO
                config.augmentedImageDatabase = AugmentedImageDatabase(session).apply {
                    markers.forEach { addImage(it.id, images.getValue(it.id), it.physicalWidthM.toFloat()) }
                }
            },
            onSessionUpdated = state::onFrame,
            onTrackingFailureChanged = { state.trackingFailure = it ?: TrackingFailureReason.NONE },
        )
        AxesOverlay(state)
        StatusBar(state, Modifier.align(Alignment.TopCenter))
    }
}

/** Machine-frame axes at the origin: red X, green Y (out of the marker), blue Z. */
@Composable
private fun AxesOverlay(state: ArState) {
    val machine = state.fix?.machineInWorld ?: return
    val viewProjection = state.viewProjection ?: return
    Canvas(Modifier.fillMaxSize()) {
        val projection = ScreenProjection(viewProjection, size.width, size.height)
        val origin = projection.project(MachineFrame.toWorld(machine, Vec3.ZERO)) ?: return@Canvas
        listOf(
            Vec3(AXIS_LENGTH_M, 0.0, 0.0) to Color.Red,
            Vec3(0.0, AXIS_LENGTH_M, 0.0) to Color.Green,
            Vec3(0.0, 0.0, AXIS_LENGTH_M) to Color.Blue,
        ).forEach { (axis, color) ->
            projection.project(MachineFrame.toWorld(machine, axis))?.let { drawLine(color, origin, it, strokeWidth = 10f) }
        }
        drawCircle(Color.White, radius = 12f, center = origin)
    }
}

@Composable
private fun StatusBar(state: ArState, modifier: Modifier) {
    val text = when {
        state.trackingFailure != TrackingFailureReason.NONE -> "Tracking: ${state.trackingFailure.name.lowercase().replace('_', ' ')}"
        state.liveMarkerId != null -> "Localised · ${state.liveMarkerId} in view"
        state.fix != null -> "Localised · marker out of view"
        else -> "Point the camera at marker-01"
    }
    Text(
        text,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .safeDrawingPadding()
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp),
    )
}
