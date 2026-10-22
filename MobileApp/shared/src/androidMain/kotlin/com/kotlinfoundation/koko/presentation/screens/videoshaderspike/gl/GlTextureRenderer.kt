package com.kotlinfoundation.koko.presentation.screens.videoshaderspike.gl

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.kotlinfoundation.koko.presentation.screens.videoshaderspike.VideoShaders

/**
 * Draws an OES camera texture through a GLSL fragment shader onto whichever EGL surface is
 * currently current. Must be constructed and used only from the GL thread (holds a live GL
 * program + texture handle). Swap [fragmentShader] (via [recompile]) to change the effect
 * without re-creating the texture/EGL state around it.
 */
internal class GlTextureRenderer(private var fragmentShader: String = VideoShaders.ditherFragmentShader(blockSize = 1)) {

    val textureId: Int = createExternalOesTexture()

    private var program = GlUtil.buildProgram(VideoShaders.VERTEX_SHADER, fragmentShader)

    // Reused, mutable buffers — geometry changes per output (different canvas aspect ratio for
    // preview vs. encoder surface) and whenever the user picks a different crop, but reusing the
    // same backing buffers avoids a per-frame allocation.
    private val positionBuffer = GlUtil.emptyQuadBuffer()
    private val texCoordBuffer = GlUtil.emptyQuadBuffer()

    /** Re-links the GL program with a new fragment shader (e.g. switching passthrough -> dither). */
    fun recompile(newFragmentShader: String) {
        if (newFragmentShader == fragmentShader) return
        GLES20.glDeleteProgram(program)
        fragmentShader = newFragmentShader
        program = GlUtil.buildProgram(VideoShaders.VERTEX_SHADER, fragmentShader)
    }

    /**
     * Draws the current OES texture frame into the bound EGL surface at [width]x[height].
     * [positions]/[texCoords] are this output's crop+letterbox geometry (see [GlGeometry]) — 4
     * vertices, BL/BR/TL/TR, matching the triangle-strip draw order.
     */
    fun drawFrame(
        textureMatrix: FloatArray,
        width: Int,
        height: Int,
        positions: FloatArray,
        texCoords: FloatArray,
    ) {
        positionBuffer.put(positions).position(0)
        texCoordBuffer.put(texCoords).position(0)

        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, positionBuffer)

        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        val matrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, textureMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    fun release() {
        GLES20.glDeleteProgram(program)
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
    }

    private fun createExternalOesTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        return textures[0]
    }
}
