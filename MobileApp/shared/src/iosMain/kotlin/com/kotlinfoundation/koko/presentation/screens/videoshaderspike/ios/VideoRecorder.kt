package com.kotlinfoundation.koko.presentation.screens.videoshaderspike.ios

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVAssetWriterInputPixelBufferAdaptor
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeH264
import platform.AVFoundation.AVVideoHeightKey
import platform.AVFoundation.AVVideoWidthKey
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreMedia.CMTime
import platform.CoreVideo.CVPixelBufferPoolCreatePixelBuffer
import platform.CoreVideo.CVPixelBufferPoolRef
import platform.CoreVideo.CVPixelBufferRef
import platform.CoreVideo.CVPixelBufferRefVar
import platform.CoreVideo.kCVPixelBufferHeightKey
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelBufferWidthKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL

/**
 * `AVAssetWriter` wrapper — mirrors Android's `Recorder`/`VideoCapture`: real-time baked-in-shader
 * recording, not a raw capture + offline re-encode. Each processed `CIImage` is rendered straight
 * into the writer's pixel buffer pool and appended with its original frame timestamp.
 */
@OptIn(ExperimentalForeignApi::class)
internal class VideoRecorder(private val ciContext: CIContext) {

    private var writer: AVAssetWriter? = null
    private var input: AVAssetWriterInput? = null
    private var adaptor: AVAssetWriterInputPixelBufferAdaptor? = null
    private var sessionStarted = false
    var outputUrl: NSURL? = null
        private set

    fun start(width: Int, height: Int) {
        val fileName = "koko-video-spike-${kotlin.random.Random.nextInt()}.mp4"
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
        outputUrl = url

        val newWriter = AVAssetWriter(url, AVFileTypeMPEG4, null)
        val videoSettings = mapOf<Any?, Any?>(
            AVVideoCodecKey to AVVideoCodecTypeH264,
            AVVideoWidthKey to width,
            AVVideoHeightKey to height,
        )
        val newInput = AVAssetWriterInput(mediaType = AVMediaTypeVideo, outputSettings = videoSettings).apply {
            expectsMediaDataInRealTime = true
        }
        val pixelBufferAttributes = mapOf<Any?, Any?>(
            kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA,
            kCVPixelBufferWidthKey to width,
            kCVPixelBufferHeightKey to height,
        )
        val newAdaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput = newInput,
            sourcePixelBufferAttributes = pixelBufferAttributes,
        )

        newWriter.addInput(newInput)
        newWriter.startWriting()

        writer = newWriter
        input = newInput
        adaptor = newAdaptor
        sessionStarted = false
    }

    fun appendFrame(image: CIImage, presentationTime: CValue<CMTime>) {
        val currentAdaptor = adaptor ?: return
        val currentInput = input ?: return
        val currentWriter = writer ?: return
        if (!currentInput.readyForMoreMediaData) return

        if (!sessionStarted) {
            currentWriter.startSessionAtSourceTime(presentationTime)
            sessionStarted = true
        }

        val pool = currentAdaptor.pixelBufferPool ?: return
        val pixelBuffer = createPooledPixelBuffer(pool) ?: return
        ciContext.render(image, toCVPixelBuffer = pixelBuffer)
        currentAdaptor.appendPixelBuffer(pixelBuffer, withPresentationTime = presentationTime)
    }

    fun stop(onFinished: (NSURL?) -> Unit) {
        val currentWriter = writer
        input?.markAsFinished()
        if (currentWriter == null) {
            onFinished(null)
            return
        }
        currentWriter.finishWritingWithCompletionHandler {
            onFinished(outputUrl)
            writer = null
            input = null
            adaptor = null
        }
    }

    private fun createPooledPixelBuffer(pool: CVPixelBufferPoolRef): CVPixelBufferRef? = memScoped {
        val pixelBufferVar = alloc<CVPixelBufferRefVar>()
        val status = CVPixelBufferPoolCreatePixelBuffer(null, pool, pixelBufferVar.ptr)
        if (status == 0) pixelBufferVar.value else null
    }
}
