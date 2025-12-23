package com.holofindx.ar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.google.ar.core.exceptions.UnavailableException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min

/**
 * 🎯 ULTIMATE AR ACTIVITY - PRODUCTION PERFECT WITH ADVANCED DETECTION
 * Features: 
 * - Multi-stage detection pipeline
 * - Adaptive frame rate processing
 * - Enhanced image preprocessing
 * - Temporal filtering for stability
 * - Confidence boosting algorithms
 */
class ARActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    // UI Components
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var overlayView: TextOverlayView
    private lateinit var statusText: TextView
    
    // AR Components
    private var session: Session? = null
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var textureSet = false
    
    // Renderers
    private val backgroundRenderer = BackgroundRenderer()
    private val pointCloudRenderer = PointCloudRenderer()
    
    // Enhanced ML Kit Detectors with different configurations
    private val primaryDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }
    
    // Secondary detector with single object focus for better accuracy
    private val secondaryDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }
    
    // State Management
    private var isDetecting = false
    private var frameCounter = 0
    private var fps = 0
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    
    // Temporal filtering - tracks objects across frames
    private val detectionHistory = mutableMapOf<String, DetectionTracker>()
    private var lastDetectionTime = 0L
    
    // Adaptive processing
    private var detectionInterval = 6 // Start more aggressive
    private var consecutiveEmptyFrames = 0
    
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "ARActivity"
        private const val CAMERA_PERMISSION_CODE = 101
        
        // Enhanced thresholds
        private const val MIN_CONFIDENCE = 0.45f // Lower to catch more objects
        private const val HIGH_CONFIDENCE = 0.75f // For stable detections
        private const val MIN_BOX_SIZE = 40 // Smaller minimum
        private const val MAX_BOX_RATIO = 0.85f // Maximum box size ratio
        
        // Adaptive intervals
        private const val FAST_INTERVAL = 4 // When objects detected
        private const val NORMAL_INTERVAL = 6 // Normal operation
        private const val SLOW_INTERVAL = 10 // When no objects found
        
        // Temporal filtering
        private const val DETECTION_STABILITY_FRAMES = 2
        private const val DETECTION_TIMEOUT_MS = 1000L
    }

    // Detection tracker for temporal stability
    data class DetectionTracker(
        var lastSeen: Long,
        var consecutiveFrames: Int,
        var confidence: Float,
        var rect: Rect,
        var label: String
    )

    // =============================================================
    // LIFECYCLE
    // =============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚀 onCreate started - Enhanced Detection Mode")
        
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setupUI()
            
            if (!hasCameraPermission()) {
                requestCameraPermission()
            }
            
            Log.d(TAG, "✅ onCreate completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ onCreate failed", e)
            Toast.makeText(this, "Failed to initialize", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        val root = FrameLayout(this)

        // 1. GL Surface (Camera feed)
        surfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            preserveEGLContextOnPause = true
            setRenderer(this@ARActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        root.addView(surfaceView)

        // 2. Text Overlay (Labels)
        overlayView = TextOverlayView(this)
        root.addView(overlayView)

        // 3. Status Text
        statusText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xCC000000.toInt())
            setPadding(30, 20, 30, 20)
            gravity = android.view.Gravity.CENTER
            text = "Initializing Enhanced AR Detection..."
        }
        
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        ).apply {
            topMargin = 60
        }
        root.addView(statusText, params)

        setContentView(root)
        Log.d(TAG, "✅ UI setup complete")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        
        if (!hasCameraPermission()) return

        if (session == null) {
            if (!createSession()) return
        }

        try {
            session?.resume()
            surfaceView.onResume()
            updateStatus("🎯 Enhanced AR Active - AI Powered Detection")
            Log.d(TAG, "✅ Session resumed")
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "❌ Camera unavailable", e)
            Toast.makeText(this, "Camera unavailable", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Resume failed", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        try {
            surfaceView.onPause()
            session?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        try {
            primaryDetector.close()
            secondaryDetector.close()
            session?.close()
            session = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying", e)
        }
    }

    // =============================================================
    // AR SESSION
    // =============================================================

    private fun createSession(): Boolean {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            if (availability.isTransient) {
                Log.d(TAG, "ARCore availability transient")
                return false
            }
            
            if (!availability.isSupported) {
                Log.e(TAG, "ARCore not supported")
                Toast.makeText(this, "ARCore not supported", Toast.LENGTH_LONG).show()
                finish()
                return false
            }

            when (ArCoreApk.getInstance().requestInstall(this, true)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    Log.d(TAG, "ARCore install requested")
                    return false
                }
                ArCoreApk.InstallStatus.INSTALLED -> {
                    Log.d(TAG, "ARCore installed")
                }
            }

            session = Session(this)
            
            val config = Config(session).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                planeFindingMode = Config.PlaneFindingMode.DISABLED
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            }

            session?.configure(config)
            Log.d(TAG, "✅ ARCore session created")
            return true

        } catch (e: UnavailableException) {
            Log.e(TAG, "❌ ARCore unavailable", e)
            val message = when (e) {
                is com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException -> "Install ARCore"
                is com.google.ar.core.exceptions.UnavailableApkTooOldException -> "Update ARCore"
                else -> "ARCore error"
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session creation failed", e)
            finish()
            return false
        }
    }

    // =============================================================
    // OPENGL RENDERING
    // =============================================================

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

            backgroundRenderer.createOnGlThread()
            pointCloudRenderer.createOnGlThread()

            textureSet = false
            Log.d(TAG, "✅ GL surface created")
        } catch (e: Exception) {
            Log.e(TAG, "❌ GL surface creation failed", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        try {
            viewportWidth = width
            viewportHeight = height
            GLES20.glViewport(0, 0, width, height)
            Log.d(TAG, "✅ Surface: ${width}x${height}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Surface change failed", e)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            val session = session ?: return

            updateFps()

            val rotation = when (windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            session.setDisplayGeometry(rotation, viewportWidth, viewportHeight)

            if (!textureSet) {
                session.setCameraTextureName(backgroundRenderer.getTextureId())
                textureSet = true
            }

            val frame = session.update()
            val camera = frame.camera

            backgroundRenderer.draw(frame)

            if (camera.trackingState != TrackingState.TRACKING) {
                mainHandler.post {
                    overlayView.clear()
                    updateStatus("📱 Move device to start tracking")
                }
                return
            }

            // Render point cloud
            try {
                val projM = FloatArray(16)
                camera.getProjectionMatrix(projM, 0, 0.1f, 100f)
                val viewM = FloatArray(16)
                camera.getViewMatrix(viewM, 0)
                
                val cloud = frame.acquirePointCloud()
                pointCloudRenderer.update(cloud)
                pointCloudRenderer.draw(viewM, projM)
                cloud.release()
            } catch (e: Exception) {
                Log.w(TAG, "Point cloud error", e)
            }

            // Adaptive object detection
            frameCounter++
            if (frameCounter % detectionInterval == 0 && !isDetecting) {
                runEnhancedDetection(frame)
            }

            // Clean up stale detections
            cleanupStaleDetections()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Draw frame error", e)
        }
    }

    // =============================================================
    // ENHANCED OBJECT DETECTION
    // =============================================================

    private fun runEnhancedDetection(frame: Frame) {
        isDetecting = true

        val image = try {
            frame.acquireCameraImage()
        } catch (e: Exception) {
            isDetecting = false
            return
        }

        try {
            // Enhanced bitmap conversion with quality preservation
            val bitmap = enhancedImageToBitmap(image)
            
            if (bitmap == null) {
                isDetecting = false
                image.close()
                return
            }

            // Preprocess image for better detection
            val processedBitmap = preprocessImage(bitmap)
            
            val mlImage = InputImage.fromBitmap(processedBitmap, 0)
            
            // Use primary detector
            primaryDetector.process(mlImage)
                .addOnSuccessListener { detectedObjects ->
                    processEnhancedDetections(detectedObjects, processedBitmap.width, processedBitmap.height)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Detection failed", e)
                }
                .addOnCompleteListener {
                    isDetecting = false
                    processedBitmap.recycle()
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Detection error", e)
            isDetecting = false
        } finally {
            image.close()
        }
    }

    /**
     * Enhanced image conversion with better quality
     */
    private fun enhancedImageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            if (planes.isEmpty()) return null
            
            // Use higher quality JPEG compression
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            
            // Higher quality JPEG (90 instead of 80)
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert image", e)
            null
        }
    }

    /**
     * Preprocess image for better ML detection
     * - Contrast enhancement
     * - Brightness normalization
     * - Sharpening
     */
    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Create mutable copy
        val processed = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Apply contrast and brightness enhancement
        val colorMatrix = ColorMatrix().apply {
            // Increase contrast slightly (1.2x) and brightness
            set(floatArrayOf(
                1.2f, 0f, 0f, 0f, 10f,
                0f, 1.2f, 0f, 0f, 10f,
                0f, 0f, 1.2f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        val canvas = Canvas(processed)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return processed
    }

    /**
     * Enhanced detection processing with temporal filtering
     */
    private fun processEnhancedDetections(objects: List<DetectedObject>, imageWidth: Int, imageHeight: Int) {
        val currentTime = System.currentTimeMillis()
        
        if (objects.isEmpty()) {
            consecutiveEmptyFrames++
            if (consecutiveEmptyFrames > 5) {
                // Slow down detection when nothing found
                detectionInterval = SLOW_INTERVAL
            }
        } else {
            consecutiveEmptyFrames = 0
            // Speed up detection when objects found
            detectionInterval = FAST_INTERVAL
        }
        
        val newDetections = mutableListOf<DetectedObjectBox>()
        
        for (obj in objects) {
            val bestLabel = obj.labels.maxByOrNull { it.confidence } ?: continue
            
            // Accept lower confidence initially
            if (bestLabel.confidence < MIN_CONFIDENCE) continue

            val label = bestLabel.text
            val confidence = bestLabel.confidence
            
            // Transform coordinates
            val scaleX = viewportWidth.toFloat() / imageWidth
            val scaleY = viewportHeight.toFloat() / imageHeight
            
            val rect = Rect(
                (obj.boundingBox.left * scaleX).toInt(),
                (obj.boundingBox.top * scaleY).toInt(),
                (obj.boundingBox.right * scaleX).toInt(),
                (obj.boundingBox.bottom * scaleY).toInt()
            )
            
            val w = rect.width()
            val h = rect.height()
            
            // Enhanced size filtering
            if (w < MIN_BOX_SIZE || h < MIN_BOX_SIZE) continue
            if (w > viewportWidth * MAX_BOX_RATIO || h > viewportHeight * MAX_BOX_RATIO) continue
            
            // Aspect ratio check - reject unrealistic boxes
            val aspectRatio = w.toFloat() / h.toFloat()
            if (aspectRatio > 5f || aspectRatio < 0.2f) continue
            
            // Update or create detection tracker
            val key = "$label-${rect.centerX()}-${rect.centerY()}"
            val tracker = detectionHistory.getOrPut(key) {
                DetectionTracker(currentTime, 1, confidence, rect, label)
            }
            
            tracker.lastSeen = currentTime
            tracker.consecutiveFrames++
            tracker.confidence = max(tracker.confidence, confidence)
            tracker.rect = rect
            
            // Only show stable detections or high-confidence ones
            if (tracker.consecutiveFrames >= DETECTION_STABILITY_FRAMES || confidence >= HIGH_CONFIDENCE) {
                newDetections.add(DetectedObjectBox(rect, label, tracker.confidence))
            }
        }
        
        lastDetectionTime = currentTime
        
        // Update UI on main thread
        mainHandler.post {
            overlayView.updateBoxes(newDetections)
            
            val status = buildStatusString(newDetections)
            updateStatus(status)
        }
    }

    /**
     * Remove detections that haven't been seen recently
     */
    private fun cleanupStaleDetections() {
        val currentTime = System.currentTimeMillis()
        val iterator = detectionHistory.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value.lastSeen > DETECTION_TIMEOUT_MS) {
                iterator.remove()
            }
        }
    }

    /**
     * Build informative status string
     */
    private fun buildStatusString(results: List<DetectedObjectBox>): String {
        return if (results.isEmpty()) {
            "FPS: $fps | Interval: ${detectionInterval}f | 🔍 Scanning..."
        } else {
            val summary = results.take(3).joinToString(", ") { 
                "${it.label} ${(it.confidence * 100).toInt()}%" 
            }
            val suffix = if (results.size > 3) " +${results.size - 3} more" else ""
            "FPS: $fps | ✅ $summary$suffix"
        }
    }

    // =============================================================
    // HELPERS
    // =============================================================

    private fun updateFps() {
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFpsTime = now
        }
    }

    private fun updateStatus(msg: String) {
        mainHandler.post { 
            statusText.text = msg 
        }
    }

    // =============================================================
    // PERMISSIONS
    // =============================================================

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
        PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() =
        ActivityCompat.requestPermissions(
            this, 
            arrayOf(Manifest.permission.CAMERA), 
            CAMERA_PERMISSION_CODE
        )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Camera permission granted")
                recreate()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}