package com.hzncc.kevin.robot_ir.textures

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.hzncc.kevin.frareddemo.R
import com.hzncc.kevin.appdemo.TextResourceReader
import com.hzncc.kevin.robot_ir.getByteBuffer
import com.hzncc.kevin.robot_ir.loadShader
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * Robot
 * Created by 蔡雨峰 on 2018/1/16.
 */
class TextureRGB {
    private var _program: Int = -1
    private var _tid: Int = -1
    private var attribPosition: Int = -1
    private var attribTexCoord: Int = -1
    private var uniformTexture: Int = -1
    var isProgramBuilt: Boolean = false
    private var _vertex_buffer: ByteBuffer? = null
    private var _coord_buffer: ByteBuffer? = null

    fun buildProgram(context: Context) {
        _vertex_buffer = getByteBuffer(squareVertices)
        _coord_buffer = getByteBuffer(coordVertices)
        if (_program <= 0) {
            val vertex_shader = TextResourceReader.readTextFileFromResource(context, R.raw.vertex_shader_rgb)
            val fragment_shader = TextResourceReader.readTextFileFromResource(context, R.raw.fragment_shader_rgb)
            _program = createProgram(vertex_shader, fragment_shader)
        }
        getHandles()
        isProgramBuilt = true
    }

    /**
     * create program and load shaders, fragment shader is very important.
     */
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        // create shaders
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        // just check
        Log.d("tag", "vertexShader = " + vertexShader)
        Log.d("tag", "pixelShader = " + pixelShader)

        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            checkGlError("glAttachShader")
            GLES20.glAttachShader(program, pixelShader)
            checkGlError("glAttachShader")
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("tag", "Could not link program: ", null)
                Log.e("tag", GLES20.glGetProgramInfoLog(program), null)
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    fun getHandles() {
        /*
        * get handle for "vPosition" and "a_texCoord"
        */
        attribPosition = GLES20.glGetAttribLocation(_program, "a_position")
        Log.d("tag", "attribPosition = " + attribPosition)
        checkGlError("glGetAttribLocation vPosition")
        if (attribPosition == -1) {
            throw RuntimeException("Could not get attribute location for vPosition")
        }
        attribTexCoord = GLES20.glGetAttribLocation(_program, "a_texCoord")
        Log.d("tag", "attribTexCoord = " + attribTexCoord)
        checkGlError("glGetAttribLocation a_texCoord")
        if (attribTexCoord == -1) {
            throw RuntimeException("Could not get attribute location for a_texCoord")
        }

        /*
        * get uniform location for y/u/v, we pass data through these uniforms
        */
        uniformTexture = GLES20.glGetUniformLocation(_program, "u_samplerTexture")
        Log.d("tag", "uniformTexture = " + uniformTexture)
        checkGlError("glGetUniformLocation uniformTexture")
        if (uniformTexture == -1) {
            throw RuntimeException("Could not get uniform location for tex_y")
        }
    }

    /**
     * build a set of textures, one for Y, one for U, and one for V.
     */
    fun buildTextures(buffer: ShortBuffer, width: Int, height: Int) {
        // building texture for Y data
        if (_tid < 0) {
            if (_tid >= 0) {
                Log.d("tag", "glDeleteTextures Y")
                GLES20.glDeleteTextures(1, intArrayOf(_tid), 0)
                checkGlError("glDeleteTextures")
            }
            // GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            checkGlError("glGenTextures")
            _tid = textures[0]
            Log.d("tag", "glGenTextures Y = " + _tid)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _tid)
        checkGlError("glBindTexture")
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, width, height, 0,
                GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, buffer)
        checkGlError("glTexImage2D")
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    /**
     * render the frame
     * the YUV data will be converted to RGB by shader.
     */
    fun drawFrame() {
        GLES20.glUseProgram(_program)
        checkGlError("glUseProgram")

        GLES20.glVertexAttribPointer(attribPosition, 2, GLES20.GL_FLOAT, false, 8, _vertex_buffer)
        checkGlError("glVertexAttribPointer mPositionHandle")
        GLES20.glEnableVertexAttribArray(attribPosition)

        GLES20.glVertexAttribPointer(attribTexCoord, 2, GLES20.GL_FLOAT, false, 8, _coord_buffer)
        checkGlError("glVertexAttribPointer maTextureHandle")
        GLES20.glEnableVertexAttribArray(attribTexCoord)

        // bind textures
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _tid)
        GLES20.glUniform1i(uniformTexture, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFinish()

        GLES20.glDisableVertexAttribArray(attribPosition)
        GLES20.glDisableVertexAttribArray(attribTexCoord)
    }

    private fun checkGlError(op: String) {
        val error: Int = GLES20.glGetError()
        while (error != GLES20.GL_NO_ERROR) {
            Log.e("tag", "***** $op: glError $error", null)
            throw RuntimeException(op + ": glError " + error)
        }
    }

    companion object {

        private val lBold = 0.04f
        private val rBold = 0.91f

        private val squareVertices = floatArrayOf(-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f) // fullscreen

        private val coordVertices = floatArrayOf(0.0f, rBold, 1.0f, rBold, 0.0f, lBold, 1.0f, lBold)// whole-texture
    }
}