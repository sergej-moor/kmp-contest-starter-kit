package com.kotlinfoundation.koko.presentation.screens.videoshaderspike

import android.content.ContentValues
import android.provider.MediaStore
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kotlinfoundation.koko.presentation.screens.videoshaderspike.gl.ShaderCameraEffect
import com.kotlinfoundation.koko.presentation.screens.videoshaderspike.gl.ShaderSurfaceProcessor
import com.kotlinfoundation.koko.util.permissions.RequestPermissionOnEntry
import com.kotlinfoundation.koko.util.permissions.rememberCameraPermissionState
import kotlinx.coroutines.delay

/**
 * Live-encoding spike screen: CameraX `Preview` + `VideoCapture<Recorder>` bound together with a
 * [ShaderSurfaceProcessor] via `CameraEffect` (Camposer doesn't expose this hook, so this screen
 * binds CameraX directly). Aspect ratio and quality options are read from this device's actual
 * camera capabilities ([queryAvailableVideoFormats]) — never a fixed list that might not exist on
 * a given device (true 1:1 video capture, for example, is sensor-dependent and often unavailable).
 * Shader and format change live without rebinding; format also rebuilds the `Recorder` (bitrate is
 * baked into CameraX's `Quality` presets) and rebinds the whole `UseCaseGroup`.
 */
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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRecording by remember { mutableStateOf(false) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var statusText by remember { mutableStateOf("Bereit") }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    var availableFormats by remember { mutableStateOf<List<VideoFormatOption>>(emptyList()) }
    var selectedFormat by remember { mutableStateOf<VideoFormatOption?>(null) }
    var selectedShader by remember { mutableStateOf(VideoShaderOption.DITHER) }
    var ditherBlockSize by remember { mutableIntStateOf(1) }

    val shaderProcessor = remember { ShaderSurfaceProcessor() }
    val preview = remember { Preview.Builder().build() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(shaderProcessor) {
        onDispose { shaderProcessor.release() }
    }

    // Read this device's real camera capabilities once, before binding anything.
    LaunchedEffect(Unit) {
        val cameraInfo = ProcessCameraProvider.getInstance(context).get()
            .getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)
        val formats = queryAvailableVideoFormats(cameraInfo)
        availableFormats = formats
        selectedFormat = formats.firstOrNull()
    }

    LaunchedEffect(selectedShader, ditherBlockSize) {
        shaderProcessor.setFragmentShader(
            when (selectedShader) {
                VideoShaderOption.NONE -> VideoShaders.FRAGMENT_SHADER_PASSTHROUGH
                VideoShaderOption.DITHER -> VideoShaders.ditherFragmentShader(ditherBlockSize)
            },
        )
    }

    LaunchedEffect(selectedFormat) {
        selectedFormat?.let { shaderProcessor.setTargetAspectRatio(it.aspectRatio) }
    }

    val recorder = remember(selectedFormat) {
        selectedFormat?.let { format ->
            Recorder.Builder().setQualitySelector(QualitySelector.from(format.quality)).build()
        }
    }
    val videoCapture = remember(recorder) { recorder?.let { VideoCapture.withOutput(it) } }

    // Format changes rebuild the Recorder (bitrate is baked into CameraX's Quality presets), so
    // the whole UseCaseGroup needs to be rebound — Preview, the CameraEffect and the GL state
    // itself stay untouched, only the encoder-facing VideoCapture use case is new.
    LaunchedEffect(previewView, videoCapture) {
        val view = previewView ?: return@LaunchedEffect
        val capture = videoCapture ?: return@LaunchedEffect
        preview.surfaceProvider = view.surfaceProvider

        val cameraEffect = ShaderCameraEffect(
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            shaderProcessor.glExecutor,
            shaderProcessor,
        ) { throwable -> android.util.Log.e("VideoShaderSpike", "CameraEffect error", throwable) }
        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(capture)
            .addEffect(cameraEffect)
            .build()

        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, useCaseGroup)
    }

    LaunchedEffect(isRecording) {
        recordingSeconds = 0
        while (isRecording) {
            delay(1000)
            recordingSeconds++
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> PreviewView(ctx).also { previewView = it } },
        )

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            SettingsRow(
                enabled = !isRecording,
                availableFormats = availableFormats,
                selectedFormat = selectedFormat,
                onFormatSelected = { selectedFormat = it },
                selectedShader = selectedShader,
                onShaderSelected = { selectedShader = it },
                ditherBlockSize = ditherBlockSize,
                onDitherBlockSizeSelected = { ditherBlockSize = it },
            )

            Text(
                text = if (isRecording) "${formatSeconds(recordingSeconds)} • $statusText" else statusText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Button(
                modifier = Modifier.padding(16.dp),
                enabled = selectedFormat != null,
                onClick = {
                    val currentRecording = activeRecording
                    val currentRecorder = recorder
                    if (currentRecording != null) {
                        currentRecording.stop()
                        activeRecording = null
                        isRecording = false
                    } else if (currentRecorder != null) {
                        val name = "koko-video-spike-${System.currentTimeMillis()}.mp4"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Video.Media.DISPLAY_NAME, name)
                            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        }
                        val outputOptions = MediaStoreOutputOptions.Builder(
                            context.contentResolver,
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        ).setContentValues(contentValues).build()

                        activeRecording = currentRecorder
                            .prepareRecording(context, outputOptions)
                            .start(ContextCompat.getMainExecutor(context)) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> statusText = "Aufnahme läuft…"

                                    is VideoRecordEvent.Finalize -> {
                                        statusText = if (!event.hasError()) {
                                            "Gespeichert: ${event.outputResults.outputUri}"
                                        } else {
                                            "Fehler: ${event.error}"
                                        }
                                    }

                                    else -> Unit
                                }
                            }
                        isRecording = true
                    }
                },
            ) {
                Text(if (isRecording) "Stop" else "Aufnahme starten")
            }
        }
    }
}

/** Dither block size options — 1 = original per-pixel grid, larger = coarser, compression-friendlier. */
private val DITHER_BLOCK_SIZES = listOf(1, 2, 4, 8, 16, 32, 64)

@Composable
private fun SettingsRow(
    enabled: Boolean,
    availableFormats: List<VideoFormatOption>,
    selectedFormat: VideoFormatOption?,
    onFormatSelected: (VideoFormatOption) -> Unit,
    selectedShader: VideoShaderOption,
    onShaderSelected: (VideoShaderOption) -> Unit,
    ditherBlockSize: Int,
    onDitherBlockSizeSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Distinct aspect ratios this device's camera actually reports (e.g. just "16:9" on many
        // phones — "1:1"/"9:16" only appear here if the sensor genuinely offers that shape).
        val aspectRatios = availableFormats.map { it.aspectLabel }.distinct()
        val selectedAspectLabel = selectedFormat?.aspectLabel
        ChipGroup(aspectRatios, selectedAspectLabel, enabled, { it }) { label ->
            availableFormats.firstOrNull { it.aspectLabel == label }?.let(onFormatSelected)
        }

        // Qualities within the selected aspect ratio (resolution/bitrate choice).
        val qualitiesForRatio = availableFormats.filter { it.aspectLabel == selectedAspectLabel }
        ChipGroup(qualitiesForRatio, selectedFormat, enabled, { it.qualityLabel }, onFormatSelected)

        ChipGroup(VideoShaderOption.entries, selectedShader, enabled, { it.label }, onShaderSelected)

        if (selectedShader == VideoShaderOption.DITHER) {
            ChipGroup(DITHER_BLOCK_SIZES, ditherBlockSize, enabled, { "Raster ${it}px" }, onDitherBlockSizeSelected)
        }
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
    return "%d:%02d".format(minutes, seconds)
}
