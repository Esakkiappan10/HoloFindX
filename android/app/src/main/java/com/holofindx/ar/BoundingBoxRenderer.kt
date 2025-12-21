package com.holofindx.ar

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Professional bounding box renderer with labels and confidence scores
 */
class BoundingBoxRenderer {

    private val boxes = mutableListOf<DetectedObjectBox>()
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    
    // Text rendering
    private val textPaint = Paint().apply {
        color = 0xFF00FF00.toInt() // Green
        textSize = 40f
        isAntiAlias = true
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
    }
    
    private val bgPaint = Paint().apply {
        color = 0xDD000000.toInt() // Semi-transparent black
        style = Paint.Style.FILL
    }
    
    companion object {
        private const val TAG = "BoundingBoxRenderer"
        private const val LINE_WIDTH = 5f
        
        // Color palette for different object types
        private val COLOR_MAP = mapOf(
            "person" to floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),      // Green
            "car" to floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f),          // Red
            "chair" to floatArrayOf(0.0f, 0.5f, 1.0f, 1.0f),        // Blue
            "cup" to floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f),          // Yellow
            "laptop" to floatArrayOf(1.0f, 0.0f, 1.0f, 1.0f),       // Magenta
            "phone" to floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f),        // Cyan
            "book" to floatArrayOf(1.0f, 0.5f, 0.0f, 1.0f),         // Orange
        )
        
        private val DEFAULT_COLOR = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f) // Green
    }

    fun createOnGlThread() {
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

        try {
            program = createProgram(vertexShader, fragmentShader)
            positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
            colorHandle = GLES20.glGetUniformLocation(program, "u_Color")
            
            Log.d(TAG, "✅ BoundingBoxRenderer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create BoundingBoxRenderer", e)
            throw e
        }
    }

    fun updateBoxes(newBoxes: List<DetectedObjectBox>) {
        synchronized(boxes) {
            boxes.clear()
            boxes.addAll(newBoxes)
            
            if (boxes.isNotEmpty()) {
                Log.d(TAG, "📦 Updated with ${boxes.size} objects:")
                boxes.forEachIndexed { index, box ->
                    Log.d(TAG, "  [$index] ${box.label} - ${(box.confidence * 100).toInt()}% confidence")
                }
            }
        }
    }

    fun draw(viewportWidth: Int, viewportHeight: Int) {
        if (boxes.isEmpty()) return
        
        GLES20.glUseProgram(program)
        
        val wasDepthTestEnabled = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        
        GLES20.glLineWidth(LINE_WIDTH)

        synchronized(boxes) {
            for (box in boxes) {
                // Get color based on object type
                val color = COLOR_MAP[box.label.lowercase()] ?: DEFAULT_COLOR
                GLES20.glUniform4f(colorHandle, color[0], color[1], color[2], color[3])
                
                // Draw main rectangle
                drawRect(box.rect, viewportWidth, viewportHeight)
                
                // Draw corner highlights for better visibility
                drawCorners(box.rect, viewportWidth, viewportHeight, color)
            }
        }
        
        if (wasDepthTestEnabled) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        }
    }

    private fun drawRect(rect: Rect, screenWidth: Int, screenHeight: Int) {
        val left = toGLX(rect.left.toFloat(), screenWidth)
        val right = toGLX(rect.right.toFloat(), screenWidth)
        val top = toGLY(rect.top.toFloat(), screenHeight)
        val bottom = toGLY(rect.bottom.toFloat(), screenHeight)

        val vertices = floatArrayOf(
            left, top,      right, top,
            right, top,     right, bottom,
            right, bottom,  left, bottom,
            left, bottom,   left, top
        )

        val buffer = createBuffer(vertices)
        
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, buffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    /**
     * Draw corner highlights for professional look
     */
    private fun drawCorners(rect: Rect, screenWidth: Int, screenHeight: Int, color: FloatArray) {
        val left = toGLX(rect.left.toFloat(), screenWidth)
        val right = toGLX(rect.right.toFloat(), screenWidth)
        val top = toGLY(rect.top.toFloat(), screenHeight)
        val bottom = toGLY(rect.bottom.toFloat(), screenHeight)
        
        val cornerLength = 0.03f // 3% of screen
        
        // Make corners slightly brighter
        GLES20.glUniform4f(colorHandle, 
            (color[0] * 1.2f).coerceAtMost(1f),
            (color[1] * 1.2f).coerceAtMost(1f),
            (color[2] * 1.2f).coerceAtMost(1f),
            1.0f
        )
        
        GLES20.glLineWidth(LINE_WIDTH * 1.5f)
        
        // Top-left corner
        val topLeftCorner = floatArrayOf(
            left, top,
            left + cornerLength, top,
            left, top,
            left, top - cornerLength
        )
        
        // Top-right corner
        val topRightCorner = floatArrayOf(
            right, top,
            right - cornerLength, top,
            right, top,
            right, top - cornerLength
        )
        
        // Bottom-left corner
        val bottomLeftCorner = floatArrayOf(
            left, bottom,
            left + cornerLength, bottom,
            left, bottom,
            left, bottom + cornerLength
        )
        
        // Bottom-right corner
        val bottomRightCorner = floatArrayOf(
            right, bottom,
            right - cornerLength, bottom,
            right, bottom,
            right, bottom + cornerLength
        )
        
        val allCorners = topLeftCorner + topRightCorner + bottomLeftCorner + bottomRightCorner
        val buffer = createBuffer(allCorners)
        
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, buffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 16)
        GLES20.glDisableVertexAttribArray(positionHandle)
        
        GLES20.glLineWidth(LINE_WIDTH)
    }

    private fun createBuffer(vertices: FloatArray): FloatBuffer {
        return ByteBuffer
            .allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }
    }

    private fun toGLX(x: Float, width: Int): Float = (x / width) * 2.0f - 1.0f
    
    private fun toGLY(y: Float, height: Int): Float = 1.0f - (y / height) * 2.0f

    private fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            throw RuntimeException("Failed to create OpenGL program")
        }
        
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Failed to link program: $error")
        }
        
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            throw RuntimeException("Failed to create shader of type $type")
        }
        
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Failed to compile shader: $error")
        }
        
        return shader
    }
    
    /**
     * Get detected boxes with labels for external use
     */
    fun getDetectedObjects(): List<DetectedObjectBox> {
        synchronized(boxes) {
            return boxes.toList()
        }
    }
}