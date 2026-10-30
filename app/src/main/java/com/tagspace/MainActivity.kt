package com.tagspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tagspace.ar.ArScreen
import com.tagspace.marker.DEMO_MARKERS

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ArScreen(DEMO_MARKERS) }
    }
}
