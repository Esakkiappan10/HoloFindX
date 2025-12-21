package com.holofindx.ar

import android.Manifest
import android.content.pm.PackageManager
import android.media.Image
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.mlkit.vision.common.InputImage
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var overlayView: TextOverlayView
    private var session: Session? = null
    private lateinit var statusText: TextView

    private val backgroundRenderer = BackgroundRenderer()
    private val boxRenderer = BoundingBoxRenderer()
    private val pointCloudRenderer = PointCloudRenderer()

    private var viewportWidth = 0
    private var viewportHeight = 0
    private var isTextureSet = false

    private var frameCounter = 0
    private var isDetecting = false
    
    private var lastFrameTime = System.currentTimeMillis()
    private var frameCount = 0
    private var fps = 0

    companion object {
        private const val TAG = "ARActivity"
        private const val CAMERA_PERMISSION_CODE = 101
        private const val DETECTION_INTERVAL = 10
        private const val MIN_CONFIDENCE = 0.65f
        private const val MIN_BOX_SIZE = 60
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Create main layout
        val rootLayout = FrameLayout(this)

        // GL Surface View
        surfaceView = GLSurfaceView(this).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(this@ARActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        rootLayout.addView(surfaceView)

        // Text overlay view
        overlayView = TextOverlayView(this)
        rootLayout.addView(overlayView)

        // Status text
        statusText = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC000000.toInt())
            setPadding(30, 20, 30, 20)
            gravity = Gravity.CENTER
        }
        
        val statusParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            topMargin = 60
        }
        rootLayout.addView(statusText, statusParams)

        setContentView(rootLayout)

        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!hasCameraPermission()) return

        if (session == null) {
            if (!createSession()) return
        }

        try {
            session?.resume()
            updateStatus("🎯 AR Active - Move camera to detect objects")
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available", e)
            Toast.makeText(this, "Camera not available", Toast.LENGTH_LONG).show()
            session = null
            return
        }

        surfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        surfaceView.onPause()
        session?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        session = null
    }

    private fun createSession(): Boolean {
        when (ArCoreApk.getInstance().requestInstall(this, true)) {
            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> return false
            ArCoreApk.InstallStatus.INSTALLED -> {}
        }

        return try {
            session = Session(this)

            val config = Config(session).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            }

            session?.configure(config)
            Log.d(TAG, "✅ ARCore session created")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create ARCore session", e)
            Toast.makeText(this, "ARCore not supported", Toast.LENGTH_LONG).show()
            finish()
            false
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        try {
            backgroundRenderer.createOnGlThread()
            pointCloudRenderer.createOnGlThread()
            boxRenderer.createOnGlThread()
            Log.d(TAG, "✅ All renderers initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize renderers", e)
        }
        
        isTextureSet = false
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        Log.d(TAG, "📐 Viewport: ${width}x${height}")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = session ?: return

        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFrameTime = currentTime
        }

        val displayRotation = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        
        session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)

        if (!isTextureSet) {
            session.setCameraTextureName(backgroundRenderer.getTextureId())
            isTextureSet = true
        }

        try {
            val frame = session.update()
            val camera = frame.camera
            
            if (camera.trackingState == TrackingState.TRACKING) {
                val pointCloud = frame.acquirePointCloud()
                pointCloudRenderer.update(pointCloud, camera)
                pointCloud.release()

                frameCounter++
                if (frameCounter % DETECTION_INTERVAL == 0 && !isDetecting) {
                    detectObjects(frame, camera)
                }
            } else {
                updateStatus("📱 Move device to initialize tracking...")
                runOnUiThread { overlayView.clear() }
            }

            backgroundRenderer.draw(frame)
            
            if (camera.trackingState == TrackingState.TRACKING) {
                pointCloudRenderer.draw(camera)
                boxRenderer.draw(viewportWidth, viewportHeight)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onDrawFrame", e)
        }
    }

    private fun detectObjects(frame: Frame, camera: Camera) {
        val image: Image = try {
            frame.acquireCameraImage()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire camera image", e)
            return
        }

        isDetecting = true
        val rotation = getMLKitRotation()
        val inputImage = InputImage.fromMediaImage(image, rotation)

        ObjectDetectorHelper.detector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val filteredObjects = detectedObjects.mapNotNull { obj ->
                    // Filter by confidence
                    if (obj.labels.isEmpty() || obj.labels[0].confidence < MIN_CONFIDENCE) {
                        return@mapNotNull null
                    }
                    
                    try {
                        val label = obj.labels[0].text
                        val confidence = obj.labels[0].confidence
                        
                        val transformedRect = transformBoundingBox(
                            obj.boundingBox,
                            image.width,
                            image.height,
                            rotation,
                            camera
                        )
                        
                        // Filter by size
                        val boxWidth = transformedRect.width()
                        val boxHeight = transformedRect.height()
                        
                        if (boxWidth < MIN_BOX_SIZE || boxHeight < MIN_BOX_SIZE || 
                            boxWidth > viewportWidth * 0.95f || 
                            boxHeight > viewportHeight * 0.95f) {
                            return@mapNotNull null
                        }
                        
                        DetectedObjectBox(
                            rect = transformedRect,
                            label = label,
                            confidence = confidence
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing object", e)
                        null
                    }
                }
                
                boxRenderer.updateBoxes(filteredObjects)
                runOnUiThread { overlayView.updateBoxes(filteredObjects) }
                
                if (filteredObjects.isEmpty()) {
                    updateStatus("FPS: $fps | 🔍 No objects detected")
                } else {
                    val summary = filteredObjects.joinToString(", ") { 
                        "${it.label} ${(it.confidence * 100).toInt()}%" 
                    }
                    updateStatus("FPS: $fps | ✅ $summary")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ML Kit failed", e)
                updateStatus("FPS: $fps | ❌ Detection error")
            }
            .addOnCompleteListener {
                image.close()
                isDetecting = false
            }
    }

    private fun getMLKitRotation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    private fun transformBoundingBox(
        rect: android.graphics.Rect,
        imageWidth: Int,
        imageHeight: Int,
        rotation: Int,
        camera: Camera
    ): android.graphics.Rect {
        val (rotatedRect, actualWidth, actualHeight) = when (rotation) {
            90 -> Triple(
                android.graphics.Rect(
                    rect.top,
                    imageWidth - rect.right,
                    rect.bottom,
                    imageWidth - rect.left
                ),
                imageHeight,
                imageWidth
            )
            180 -> Triple(
                android.graphics.Rect(
                    imageWidth - rect.right,
                    imageHeight - rect.bottom,
                    imageWidth - rect.left,
                    imageHeight - rect.top
                ),
                imageWidth,
                imageHeight
            )
            270 -> Triple(
                android.graphics.Rect(
                    imageHeight - rect.bottom,
                    rect.left,
                    imageHeight - rect.top,
                    rect.right
                ),
                imageHeight,
                imageWidth
            )
            else -> Triple(rect, imageWidth, imageHeight)
        }
        
        val imageAspect = actualWidth.toFloat() / actualHeight
        val screenAspect = viewportWidth.toFloat() / viewportHeight
        
        val (scaleX, scaleY, offsetX, offsetY) = if (imageAspect > screenAspect) {
            val scale = viewportHeight.toFloat() / actualHeight
            val scaledWidth = actualWidth * scale
            val offset = (scaledWidth - viewportWidth) / 2f
            Quadruple(scale, scale, -offset, 0f)
        } else {
            val scale = viewportWidth.toFloat() / actualWidth
            val scaledHeight = actualHeight * scale
            val offset = (scaledHeight - viewportHeight) / 2f
            Quadruple(scale, scale, 0f, -offset)
        }
        
        return android.graphics.Rect(
            ((rotatedRect.left * scaleX) + offsetX).toInt().coerceIn(0, viewportWidth),
            ((rotatedRect.top * scaleY) + offsetY).toInt().coerceIn(0, viewportHeight),
            ((rotatedRect.right * scaleX) + offsetX).toInt().coerceIn(0, viewportWidth),
            ((rotatedRect.bottom * scaleY) + offsetY).toInt().coerceIn(0, viewportHeight)
        )
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
               PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            recreate()
        } else {
            Toast.makeText(this, "Camera permission required for AR", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)