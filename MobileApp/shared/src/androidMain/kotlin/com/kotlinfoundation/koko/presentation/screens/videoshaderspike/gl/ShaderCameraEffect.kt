package com.kotlinfoundation.koko.presentation.screens.videoshaderspike.gl

import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceProcessor
import androidx.core.util.Consumer
import java.util.concurrent.Executor

/**
 * `CameraEffect`'s constructor is `protected` — CameraX expects a subclass, not a direct
 * instance, even though [ShaderSurfaceProcessor] holds all the actual behavior.
 */
internal class ShaderCameraEffect(
    targets: Int,
    executor: Executor,
    surfaceProcessor: SurfaceProcessor,
    errorListener: Consumer<Throwable>,
) : CameraEffect(targets, executor, surfaceProcessor, errorListener)
