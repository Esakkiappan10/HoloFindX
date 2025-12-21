package com.holofindx.ar

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.Camera
import com.google.ar.core.PointCloud
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders ARCore point cloud in 3D space
 * Shows detected feature points as yellow dots
 */
class PointCloudRenderer {

    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0

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
        val vertexShader = """
            uniform mat4 u_ModelViewProjection;
            attribute vec4 a_Position;
            void main() {
                // Transform point to clip space
                gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);
                gl_PointSize = 5.0;
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
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")

            // Generate VBO for point data
            GLES20.glGenBuffers(1, vbo, 0)
            
            Log.d(TAG, "PointCloudRenderer initialized successfully")
            Log.d(TAG, "Program: $program, Position: $positionHandle, Color: $colorHandle, MVP: $mvpMatrixHandle")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create PointCloudRenderer", e)
            throw e
        }
    }

    /**
     * Update point cloud data from ARCore
     * @param pointCloud ARCore point cloud
     * @param camera ARCore camera for view/projection matrices
     */
    fun update(pointCloud: PointCloud, camera: Camera) {
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
            
            // Calculate model-view-projection matrix
            val viewMatrix = FloatArray(16)
            val projectionMatrix = FloatArray(16)
            
            // Get camera view matrix (world -> camera space)
            camera.getViewMatrix(viewMatrix, 0)
            
            // Get projection matrix (camera -> clip space)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
            
            // Combine: projection * view
            Matrix.multiplyMM(
                modelViewProjectionMatrix, 0,
                projectionMatrix, 0,
                viewMatrix, 0
            )
        }
    }

    /**
     * Render the point cloud
     * @param camera ARCore camera (unused but kept for API consistency)
     */
    fun draw(camera: Camera) {
        if (pointCount == 0) return

        GLES20.glUseProgram(program)
        
        // Disable depth writing for point cloud (allow drawing over it)
        GLES20.glDepthMask(false)

        // Set point color (yellow with slight transparency)
        GLES20.glUniform4f(colorHandle, 1.0f, 1.0f, 0.0f, 0.8f)
        
        // Set model-view-projection matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionMatrix, 0)

        // Bind VBO and set up vertex attribute
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        /**
         * 🚨 CRITICAL: Point cloud data format from ARCore
         * Format: [x, y, z, confidence, x, y, z, confidence, ...]
         * - 4 components per point (vec4)
         * - Stride = 16 bytes (4 floats * 4 bytes/float)
         * - We only use xyz, confidence is ignored by shader
         */
        GLES20.glVertexAttribPointer(
            positionHandle,
            4,                  // 4 components (x, y, z, confidence)
            GLES20.GL_FLOAT,    // type
            false,              // normalized
            16,                 // stride: 16 bytes between points
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

    /**
     * Create and link OpenGL program from vertex and fragment shaders
     */
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
        
        // Check for linking errors
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Failed to link program: $error")
        }
        
        // Shaders can be deleted after linking
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        
        return program
    }

    /**
     * Compile a shader
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            throw RuntimeException("Failed to create shader of type $type")
        }
        
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        
        // Check for compilation errors
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            val shaderType = if (type == GLES20.GL_VERTEX_SHADER) "vertex" else "fragment"
            throw RuntimeException("Failed to compile $shaderType shader: $error")
        }
        
        return shader
    }
}