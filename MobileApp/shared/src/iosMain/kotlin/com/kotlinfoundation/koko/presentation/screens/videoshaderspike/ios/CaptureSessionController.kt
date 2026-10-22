package com.kotlinfoundation.koko.presentation.screens.videoshaderspike.ios

import com.kotlinfoundation.koko.presentation.screens.videoshaderspike.VideoFormatOption
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferGetPresentationTimeStamp
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreMedia.CMTime
import platform.CoreVideo.CVImageBufferRef
import platform.UIKit.UIImage
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

/**
 * Owns the `AVCaptureSession` and the per-frame delegate callback — mirrors Android's
 * `ShaderSurfaceProcessor`, minus the GL/EGL plumbing: CoreImage's `CIContext` does the
 * GPU-accelerated rendering here instead of hand-written OpenGL.
 *
 * Preview is a plain `UIImageView` fed a fresh `UIImage` per frame (via [onPreviewFrame]) rather
 * than a custom `CAMetalLayer` view — simpler and safer to write correctly without a Mac to test
 * on; swappable for a Metal-backed view later once real device testing is possible.
 */
@OptIn(ExperimentalForeignApi::class)
internal class CaptureSessionController(
    private val ditherChain: DitherFilterChain,
    private val onPreviewFrame: (UIImage) -> Unit,
    private val onRawFrame: (CIImage, CValue<CMTime>) -> Unit,
) : NSObject(),
    AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    val session = AVCaptureSession()
    private val ciContext = CIContext(options = null)

    var ditherIntensity: Double = 1.0
    var applyFilter: Boolean = true

    fun start(format: VideoFormatOption?) {
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPresetHigh

        val device = AVCaptureDevice.defaultDeviceWithDeviceType(
            deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionBack,
        )
        if (device != null) {
            if (format != null && device.lockForConfiguration(null)) {
                device.setActiveFormat(format.format)
                device.unlockForConfiguration()
            }
            val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
            if (input != null && session.canAddInput(input)) {
                session.addInput(input)
            }
        }

        val output = AVCaptureVideoDataOutput()
        // Main queue for the spike — simplest to get right without a Mac to test on; a
        // dedicated background queue (moving CoreImage work off the UI thread) is a later perf tweak.
        output.setSampleBufferDelegate(this, queue = dispatch_get_main_queue())
        if (session.canAddOutput(output)) {
            session.addOutput(output)
        }

        session.commitConfiguration()
        session.startRunning()
    }

    fun stop() {
        session.stopRunning()
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        val sampleBuffer = didOutputSampleBuffer ?: return
        val pixelBuffer: CVImageBufferRef = CMSampleBufferGetImageBuffer(sampleBuffer) ?: return
        val sourceImage = CIImage(cVPixelBuffer = pixelBuffer)
        val processed = if (applyFilter) ditherChain.apply(sourceImage, ditherIntensity) else sourceImage

        val cgImage = ciContext.createCGImage(processed, fromRect = processed.extent)
        if (cgImage != null) {
            onPreviewFrame(UIImage(cGImage = cgImage))
        }

        val time = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
        onRawFrame(processed, time)
    }
}
