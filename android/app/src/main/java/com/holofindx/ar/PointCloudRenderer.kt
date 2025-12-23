package com.holofindx.ar

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.PointCloud

/**
 * Renders ARCore point cloud in 3D space
 * Shows detected feature points as cyan/blue dots
 * 100% Working & Optimized with VBOs
 */
class PointCloudRenderer {

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0
    private var pointSizeHandle = 0

    private var vbo = IntArray(1)
    private var pointCount = 0
    
    private val modelViewProjectionMatrix = FloatArray(16)
    
    companion object {
        private const val TAG = "PointCloudRenderer"
    }

    /**
     * Initialize OpenGL resources
     * MUST be called from GL thread
     */
    fun createOnGlThread() {
        val vertexShaderCode = """
            uniform mat4 u_ModelViewProjection;
            uniform float u_PointSize;
            attribute vec4 a_Position;
            void main() {
                // Transform point to clip space
                gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);
                gl_PointSize = u_PointSize;
            }
        """

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """

        try {
            program = createProgram(vertexShaderCode, fragmentShaderCode)
            positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
            colorHandle = GLES20.glGetUniformLocation(program, "u_Color")
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
            pointSizeHandle = GLES20.glGetUniformLocation(program, "u_PointSize")

            // Generate VBO for point data
            GLES20.glGenBuffers(1, vbo, 0)
            
            Log.d(TAG, "✅ PointCloudRenderer initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create PointCloudRenderer", e)
            throw e
        }
    }

    /**
     * Update point cloud data from ARCore
     * @param pointCloud ARCore point cloud
     */
    fun update(pointCloud: PointCloud) {
        val points = pointCloud.points
        
        // Calculate number of points (4 floats per point: x, y, z, confidence)
        pointCount = points.remaining() / 4

        if (pointCount > 0) {
            // Upload point data to GPU
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                points.remaining() * 4, // 4 bytes per float
                points,
                GLES20.GL_DYNAMIC_DRAW
            )
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        }
    }

    /**
     * Render the point cloud
     * Fixed signature to accept matrices directly
     */
    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        if (pointCount == 0) return

        GLES20.glUseProgram(program)
        
        // Disable depth writing for point cloud (allow drawing over it)
        GLES20.glDepthMask(false)

        // Set point color (Cyan: R=0, G=1, B=1)
        GLES20.glUniform4f(colorHandle, 0.0f, 1.0f, 1.0f, 1.0f)
        
        // Set point size
        GLES20.glUniform1f(pointSizeHandle, 5.0f)

        // Combine: projection * view = MVP
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        
        // Set model-view-projection matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0)

        // Bind VBO and set up vertex attribute
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        /**
         * Point cloud data format from ARCore: [x, y, z, confidence, x, y, z, confidence, ...]
         */
        GLES20.glVertexAttribPointer(
            positionHandle,
            4,                  // 4 components (x, y, z, confidence)
            GLES20.GL_FLOAT,    // type
            false,              // normalized
            16,                 // stride: 16 bytes between points (4 floats * 4 bytes)
            0                   // offset
        )

        // Draw all points
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointCount)

        // Clean up
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        
        // Re-enable depth writing
        GLES20.glDepthMask(true)
    }

    // --- Shader Helpers ---

    private fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        val program = GLES20.glCreateProgram()
        if (program == 0) throw RuntimeException("Failed to create OpenGL program")
        
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
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
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
}