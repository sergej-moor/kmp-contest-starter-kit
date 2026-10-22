package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kotlinfoundation.koko.util.permissions.RequestPermissionOnEntry
import com.kotlinfoundation.koko.util.permissions.rememberCameraPermissionState
import com.ujizin.camposer.CameraPreview
import com.ujizin.camposer.controller.camera.CameraController
import com.ujizin.camposer.session.rememberCameraSession
import com.ujizin.camposer.state.properties.ImplementationMode

@Composable
actual fun CameraShaderContent(modifier: Modifier) {
    val permissionState = rememberCameraPermissionState()
    RequestPermissionOnEntry(permissionState)

    if (!permissionState.isGranted) {
        Text(
            text = "Kamera-Freigabe wird benötigt, um den Shader-Spike zu testen.",
            modifier = modifier,
        )
        return
    }

    val controller = remember { CameraController() }
    val cameraSession = rememberCameraSession(controller)

    CameraPreview(
        modifier = modifier.fillMaxSize().liveDitherShaderEffect(),
        cameraSession = cameraSession,
        // SurfaceView (the default Performance mode) is composited by SurfaceFlinger directly and
        // ignores ancestor RenderEffect/graphicsLayer — TextureView (Compatible) draws through the
        // normal view hierarchy instead, which is what makes the shader actually visible.
        implementationMode = ImplementationMode.Compatible,
    ) {}
}
