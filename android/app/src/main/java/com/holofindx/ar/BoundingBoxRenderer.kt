package com.holofindx.ar

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BoundingBoxRenderer {

    private val boxes = mutableListOf<DetectedObjectBox>()

    private val program: Int
    private val positionHandle: Int
    private val colorHandle: Int

    init {
        val vertexShader = """
            attribute vec4 a_Position;
            void main() {
                gl_Position = a_Position;
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
    }

    fun updateBoxes(newBoxes: List<DetectedObjectBox>) {
        synchronized(boxes) {
            boxes.clear()
            boxes.addAll(newBoxes)
        }
    }

    fun draw(viewportWidth: Int, viewportHeight: Int) {
        GLES20.glUseProgram(program)
        GLES20.glLineWidth(4f)

        GLES20.glUniform4f(colorHandle, 0f, 1f, 0f, 1f) // GREEN

        synchronized(boxes) {
            for (box in boxes) {
                drawRect(box.rect, viewportWidth, viewportHeight)
            }
        }
    }

    private fun drawRect(rect: android.graphics.Rect, w: Int, h: Int) {
        val left = toGLX(rect.left.toFloat(), w)
        val right = toGLX(rect.right.toFloat(), w)
        val top = toGLY(rect.top.toFloat(), h)
        val bottom = toGLY(rect.bottom.toFloat(), h)

        val vertices = floatArrayOf(
            left, top,     right, top,
            right, top,    right, bottom,
            right, bottom, left, bottom,
            left, bottom,  left, top
        )

        val buffer: FloatBuffer = ByteBuffer
            .allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, buffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun toGLX(x: Float, width: Int): Float =
        (x / width) * 2f - 1f

    private fun toGLY(y: Float, height: Int): Float =
        1f - (y / height) * 2f

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
