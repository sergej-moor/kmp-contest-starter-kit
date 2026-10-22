package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import com.kotlinfoundation.koko.presentation.screens.videoshaderspike.ios.CaptureSessionController
import com.kotlinfoundation.koko.presentation.screens.videoshaderspike.ios.DitherFilterChain
import com.kotlinfoundation.koko.presentation.screens.videoshaderspike.ios.VideoRecorder
import com.kotlinfoundation.koko.util.permissions.RequestPermissionOnEntry
import com.kotlinfoundation.koko.util.permissions.rememberCameraPermissionState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.Foundation.NSError
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

/**
 * iOS counterpart to the Android `VideoShaderPreview` — same shape (device-queried formats, live
 * shader toggle, start/stop recording with a timer), built on AVFoundation/CoreImage instead of
 * CameraX/EGL. The dither effect here is CoreImage's built-in `CIDither` (see [DitherFilterChain])
 * rather than a custom kernel — it has no adjustable grid size, so unlike Android there's no
 * dither-raster picker here; that's an honest capability gap, not an oversight.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoShaderPreview(modifier: Modifier) {
    val permissionState = rememberCameraPermissionState()
    RequestPermissionOnEntry(permissionState)

    if (!permissionState.isGranted) {
        Text(
            text = "Kamera-Freigabe wird benötigt, um den Video-Spike zu testen.",
            modifier = modifier,
        )
        return
    }

    var isRecording by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Bereit") }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    var availableFormats by remember { mutableStateOf<List<VideoFormatOption>>(emptyList()) }
    var selectedFormat by remember { mutableStateOf<VideoFormatOption?>(null) }
    var selectedShader by remember { mutableStateOf(VideoShaderOption.DITHER) }

    var previewImage by remember { mutableStateOf<UIImage?>(null) }
    val ditherChain = remember { DitherFilterChain() }
    val recorder = remember { VideoRecorder(ciContext = platform.CoreImage.CIContext(options = null)) }

    val captureController = remember {
        CaptureSessionController(
            ditherChain = ditherChain,
            onPreviewFrame = { image -> previewImage = image },
            onRawFrame = { image, time -> if (isRecording) recorder.appendFrame(image, time) },
        )
    }

    LaunchedEffect(Unit) {
        availableFormats = queryAvailableVideoFormats()
        selectedFormat = availableFormats.firstOrNull()
    }

    LaunchedEffect(selectedShader) {
        captureController.applyFilter = selectedShader == VideoShaderOption.DITHER
    }

    LaunchedEffect(selectedFormat) {
        captureController.stop()
        captureController.start(selectedFormat)
    }

    LaunchedEffect(isRecording) {
        recordingSeconds = 0
        while (isRecording) {
            delay(1000)
            recordingSeconds++
        }
    }

    DisposableEffect(Unit) {
        onDispose { captureController.stop() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        UIKitView(
            factory = {
                UIImageView().apply { contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit }
            },
            modifier = Modifier.fillMaxSize(),
            update = { imageView -> imageView.image = previewImage },
        )

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            SettingsRow(
                enabled = !isRecording,
                availableFormats = availableFormats,
                selectedFormat = selectedFormat,
                onFormatSelected = { selectedFormat = it },
                selectedShader = selectedShader,
                onShaderSelected = { selectedShader = it },
            )

            Text(
                text = if (isRecording) "${formatSeconds(recordingSeconds)} • $statusText" else statusText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Button(
                modifier = Modifier.padding(16.dp),
                enabled = selectedFormat != null,
                onClick = {
                    val format = selectedFormat
                    if (isRecording) {
                        isRecording = false
                        recorder.stop { url ->
                            statusText = if (url != null) {
                                saveVideoToPhotoLibrary(url)
                                "Gespeichert: ${url.path}"
                            } else {
                                "Fehler beim Speichern"
                            }
                        }
                    } else if (format != null) {
                        recorder.start(format.width, format.height)
                        statusText = "Aufnahme läuft…"
                        isRecording = true
                    }
                },
            ) {
                Text(if (isRecording) "Stop" else "Aufnahme starten")
            }
        }
    }
}

private fun saveVideoToPhotoLibrary(url: platform.Foundation.NSURL) {
    PHPhotoLibrary.sharedPhotoLibrary().performChanges(
        changeBlock = { PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(url) },
        completionHandler = { _: Boolean, _: NSError? -> },
    )
}

@Composable
private fun SettingsRow(
    enabled: Boolean,
    availableFormats: List<VideoFormatOption>,
    selectedFormat: VideoFormatOption?,
    onFormatSelected: (VideoFormatOption) -> Unit,
    selectedShader: VideoShaderOption,
    onShaderSelected: (VideoShaderOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val aspectRatios = availableFormats.map { it.aspectLabel }.distinct()
        val selectedAspectLabel = selectedFormat?.aspectLabel
        ChipGroup(aspectRatios, selectedAspectLabel, enabled, { it }) { label ->
            availableFormats.firstOrNull { it.aspectLabel == label }?.let(onFormatSelected)
        }

        val qualitiesForRatio = availableFormats.filter { it.aspectLabel == selectedAspectLabel }
        ChipGroup(qualitiesForRatio, selectedFormat, enabled, { it.qualityLabel }, onFormatSelected)

        ChipGroup(VideoShaderOption.entries, selectedShader, enabled, { it.label }, onShaderSelected)
    }
}

@Composable
private fun <T> ChipGroup(
    options: List<T>,
    selected: T?,
    enabled: Boolean,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(options) { option ->
            FilterChip(
                selected = option == selected,
                enabled = enabled,
                onClick = { onSelected(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val paddedSeconds = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minutes:$paddedSeconds"
}
