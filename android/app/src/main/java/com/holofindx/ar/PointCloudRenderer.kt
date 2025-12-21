package com.holofindx.ar

import android.opengl.GLES20
import com.google.ar.core.PointCloud
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PointCloudRenderer {

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0

    private var vbo = IntArray(1)
    private var pointCount = 0

    init {
        val vertexShader = """
            attribute vec4 a_Position;
            void main() {
                gl_Position = a_Position;
                gl_PointSize = 6.0;
            }
        """

        val fragmentShader = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """

        program = createProgram(vertexShader, fragmentShader)
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetUniformLocation(program, "u_Color")

        GLES20.glGenBuffers(1, vbo, 0)
    }

    fun update(pointCloud: PointCloud) {
        val points = pointCloud.points
        pointCount = points.remaining() / 4

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            points.remaining() * 4,
            points,
            GLES20.GL_DYNAMIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        if (pointCount == 0) return

        GLES20.glUseProgram(program)

        GLES20.glUniform4f(colorHandle, 1f, 1f, 0f, 1f) // YELLOW

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            16,
            0
        )

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointCount)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun createProgram(v: String, f: String): Int {
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, v)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, f)
        return GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vs)
            GLES20.glAttachShader(this, fs)
            GLES20.glLinkProgram(this)
        }
    }

    private fun loadShader(type: Int, code: String): Int =
        GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, code)
            GLES20.glCompileShader(it)
        }
}
