package com.kotlinfoundation.koko.presentation.screens.videoshaderspike.gl

import android.graphics.SurfaceTexture
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import java.util.concurrent.Executor

/**
 * CameraX `SurfaceProcessor`: receives camera frames as an OES texture via [onInputSurface],
 * draws them through [GlTextureRenderer] on a dedicated GL thread, and writes the result to
 * every output surface CameraX hands it via [onOutputSurface] (preview + video encoder share the
 * identical draw call, so whatever shader is active is guaranteed consistent across both).
 *
 * Started as a 1:1 passthrough to validate the EGL/OES wiring in isolation, then the real dither
 * effect; [setFragmentShader] swaps the active shader and [setTargetAspectRatio] the active crop,
 * both live, without rebinding the camera.
 */
internal class ShaderSurfaceProcessor : SurfaceProcessor {

    private val glThread = HandlerThread("ShaderSurfaceProcessorGL").apply { start() }
    private val glHandler = Handler(glThread.looper)
    val glExecutor: Executor = Executor { command -> glHandler.post(command) }

    private var eglCore: EglCore? = null
    private var offscreenSurface: EGLSurface? = null
    private var renderer: GlTextureRenderer? = null
    private var cameraTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null
    private val cameraTransform = FloatArray(16)
    private val outputTransform = FloatArray(16)

    /** Set once in [onInputSurface] from the camera's actual capture resolution. */
    private var cameraAspectRatio = 16f / 9f

    /** User-selected crop, default 16:9 (no crop against a typical 16:9 camera stream). */
    @Volatile
    private var targetAspectRatio = 16f / 9f

    private data class OutputTarget(val output: SurfaceOutput, val eglSurface: EGLSurface)
    private val outputs = mutableListOf<OutputTarget>()

    /** Swap the active shader (e.g. passthrough -> dither) from any thread. */
    fun setFragmentShader(fragmentShader: String) {
        glExecutor.execute { renderer?.recompile(fragmentShader) }
    }

    /** Change the output crop (e.g. 16:9 -> 1:1) from any thread; applied on the next drawn frame. */
    fun setTargetAspectRatio(aspectRatio: Float) {
        targetAspectRatio = aspectRatio
    }

    override fun onInputSurface(request: SurfaceRequest) {
        glExecutor.execute {
            val core = eglCore ?: EglCore().also { eglCore = it }
            val offscreen = offscreenSurface ?: core.createOffscreenSurface().also { offscreenSurface = it }
            core.makeCurrent(offscreen)

            val glRenderer = renderer ?: GlTextureRenderer().also { renderer = it }

            cameraAspectRatio = request.resolution.width.toFloat() / request.resolution.height.toFloat()

            val surfaceTexture = SurfaceTexture(glRenderer.textureId).apply {
                setDefaultBufferSize(request.resolution.width, request.resolution.height)
                setOnFrameAvailableListener({ drawCurrentFrame() }, glHandler)
            }
            cameraTexture = surfaceTexture

            val surface = Surface(surfaceTexture)
            inputSurface = surface
            request.provideSurface(surface, glExecutor) {
                glExecutor.execute {
                    surface.release()
                    surfaceTexture.release()
                }
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        glExecutor.execute {
            val core = eglCore ?: EglCore().also { eglCore = it }
            val surface = surfaceOutput.getSurface(glExecutor) {
                glExecutor.execute {
                    outputs.removeAll { it.output == surfaceOutput }
                    surfaceOutput.close()
                }
            }
            val eglSurface = core.createWindowSurface(surface)
            outputs += OutputTarget(surfaceOutput, eglSurface)
        }
    }

    private fun drawCurrentFrame() {
        val core = eglCore ?: return
        val surfaceTexture = cameraTexture ?: return
        val glRenderer = renderer ?: return

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(cameraTransform)

        for (target in outputs.toList()) {
            core.makeCurrent(target.eglSurface)
            target.output.updateTransformMatrix(outputTransform, cameraTransform)
            val size = target.output.size
            val canvasAspectRatio = size.width.toFloat() / size.height.toFloat()
            val positions = GlGeometry.letterboxPositions(targetAspectRatio, canvasAspectRatio)
            val texCoords = GlGeometry.cropTexCoords(cameraAspectRatio, targetAspectRatio)
            glRenderer.drawFrame(outputTransform, size.width, size.height, positions, texCoords)
            core.swapBuffers(target.eglSurface)
        }
    }

    fun release() {
        glExecutor.execute {
            renderer?.release()
            outputs.forEach { eglCore?.destroySurface(it.eglSurface) }
            outputs.clear()
            inputSurface?.release()
            cameraTexture?.release()
            eglCore?.release()
            glThread.quitSafely()
        }
    }
}
