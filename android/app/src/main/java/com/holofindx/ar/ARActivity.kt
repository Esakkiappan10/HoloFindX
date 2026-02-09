package com.holofindx.ar

// ═══════════════════════════════════════════════════════════
// COMPLETE IMPORTS - ALL REQUIRED DEPENDENCIES
// ═══════════════════════════════════════════════════════════

// Android Core
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

// Graphics & Media
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.YuvImage
import android.media.Image

// OpenGL
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

// AndroidX
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// ARCore
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException

// Kotlin Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

// Java
import java.io.ByteArrayOutputStream

// Kotlin Math
import kotlin.math.*
/**
 * 🚀 ULTIMATE AR DETECTION ENGINE - PRODUCTION READY
 * 
 * ✅ FIXED: Runtime permission handling
 * ✅ FIXED: Android 13+ compatibility
 * ✅ FIXED: Proper initialization flow
 */
class ARActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    // UI Components
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var boxOverlayView: BoxOverlayView
    private lateinit var textOverlayView: TextOverlayView
    private lateinit var statusText: TextView
    private var yoloDetector: YoloV8Detector? = null

    // AR Components
    private var session: Session? = null
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var textureSet = false
    
    // Camera tracking
    private var cameraImageWidth = 0
    private var cameraImageHeight = 0
    private var displayRotation = 0
    
    // Renderers
    private val backgroundRenderer = BackgroundRenderer()
    private val pointCloudRenderer = PointCloudRenderer()
    
    // Coroutines
    private val detectionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // State management
    @Volatile private var isDetecting = false
    @Volatile private var isInitialized = false
    private var frameCounter = 0
    private var fps = 0
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    
    // Advanced tracking system
    private val objectTrackers = mutableMapOf<String, ObjectTracker>()
    private var lastDetectionTime = 0L
    
    // Adaptive processing
    private var detectionInterval = 3
    private var consecutiveEmptyFrames = 0
    
    // Performance monitoring
    private val detectionTimes = ArrayDeque<Long>(20)
    private var totalFrames = 0
    private var detectionFrames = 0

    companion object {
        private const val TAG = "UltimateAR"
        private const val CAMERA_PERMISSION_CODE = 101
        
        // Detection parameters
        private const val MIN_CONFIDENCE = 0.35f
        private const val MIN_BOX_SIZE = 30
        private const val MAX_BOX_RATIO = 0.90f
        private const val TRACKING_TIMEOUT_MS = 2500L
        private const val SMOOTHING_FACTOR = 0.35f
        private const val CONFIDENCE_BOOST = 0.05f
    }

    data class ObjectTracker(
        val id: String,
        var label: String,
        var confidence: Float,
        var rect: Rect,
        var smoothedRect: RectF,
        var lastSeen: Long,
        var detectionCount: Int,
        var avgConfidence: Float,
        var isStable: Boolean = false
    )

    // ═══════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "╔═══════════════════════════════════════╗")
        Log.d(TAG, "║  🚀 ULTIMATE AR DETECTION STARTING   ║")
        Log.d(TAG, "╚═══════════════════════════════════════╝")

        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setupUI()

            // ✅ Check permissions FIRST
            if (!hasAllPermissions()) {
                requestAllPermissions()
                return
            }

            // Initialize detector only if permissions granted
            initializeDetector()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Initialization failed", e)
            showErrorAndFinish("Initialization error: ${e.message}")
        }
    }

    private fun setupUI() {
        val root = FrameLayout(this)

        // GL Surface for AR camera feed
        surfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            preserveEGLContextOnPause = true
            setRenderer(this@ARActivity)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        root.addView(surfaceView)

        // Box overlay (draws bounding boxes)
        boxOverlayView = BoxOverlayView(this)
        root.addView(boxOverlayView)

        // Text overlay (draws labels and confidence)
        textOverlayView = TextOverlayView(this)
        root.addView(textOverlayView)

        // Status display with stats
        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xDD000000.toInt())
            setPadding(20, 12, 20, 12)
            gravity = android.view.Gravity.CENTER
            text = "🎯 Initializing..."
            typeface = Typeface.MONOSPACE
        }
        
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        ).apply { topMargin = 40 }
        
        root.addView(statusText, params)
        setContentView(root)
        
        Log.d(TAG, "✅ UI setup complete")
    }

    private fun initializeDetector() {
        try {
            yoloDetector = YoloV8Detector(this)
            isInitialized = true
            Log.d(TAG, "✅ Detector initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Detector initialization failed", e)
            showErrorAndFinish("Failed to initialize AI detector: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        
        try {
            // ✅ Check permissions again
            if (!hasAllPermissions()) {
                updateStatus("⚠️ Permissions required")
                return
            }

            if (!isInitialized) {
                initializeDetector()
                if (!isInitialized) return
            }

            if (session == null) {
                if (!createSession()) return
            }

            session?.resume()
            surfaceView.onResume()
            updateStatus("🎯 DETECTION ACTIVE")
            Log.d(TAG, "✅ AR session resumed")
            
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "❌ Camera not available", e)
            showErrorAndFinish("Camera error: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission denied", e)
            showErrorAndFinish("Permission denied: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "❌ Invalid argument", e)
            showErrorAndFinish("Configuration error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Resume failed", e) 
            showErrorAndFinish("Failed to start AR: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            surfaceView.onPause()
            session?.pause()
            Log.d(TAG, "⏸️ AR session paused")
        } catch (e: Exception) {
            Log.e(TAG, "Pause error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            detectionScope.cancel()
            session?.close()
            yoloDetector?.close()
            session = null
            Log.d(TAG, "✅ Cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PERMISSIONS - FIXED FOR ALL ANDROID VERSIONS
    // ═══════════════════════════════════════════════════════════

    private fun hasAllPermissions(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        Log.d(TAG, "Camera permission: ${if (cameraGranted) "✅ GRANTED" else "❌ DENIED"}")
        return cameraGranted
    }

    private fun requestAllPermissions() {
        Log.d(TAG, "📋 Requesting permissions...")
        
        val permissions = arrayOf(Manifest.permission.CAMERA)
        
        ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        Log.d(TAG, "📋 Permission result: code=$requestCode, results=${grantResults.contentToString()}")
        
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                
                Log.d(TAG, "✅ Camera permission GRANTED")
                
                // Initialize detector now
                if (!isInitialized) {
                    initializeDetector()
                }
                
                // Recreate to start fresh
                recreate()
            } else {
                Log.e(TAG, "❌ Camera permission DENIED")
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("HoloFindX needs camera access to detect objects in AR. Please grant camera permission in settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                // Open app settings
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", packageName, null)
                )
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    // ═══════════════════════════════════════════════════════════
    // AR SESSION
    // ═══════════════════════════════════════════════════════════

    private fun createSession(): Boolean {
        try {
            // Check ARCore availability
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            Log.d(TAG, "ARCore availability: $availability")
            
            when (availability) {
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                    showErrorAndFinish("ARCore not supported on this device")
                    return false
                }
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> {
                    try {
                        ArCoreApk.getInstance().requestInstall(this, true)
                    } catch (e: Exception) {
                        Log.e(TAG, "ARCore install failed: ${e.message}")
                        showErrorAndFinish("Please install ARCore from Play Store")
                    }
                    return false
                }
                ArCoreApk.Availability.UNKNOWN_CHECKING,
                ArCoreApk.Availability.UNKNOWN_ERROR,
                ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                    Log.w(TAG, "ARCore check returned: $availability")
                    // Continue anyway, might work
                }
                else -> {
                    Log.d(TAG, "ARCore available: $availability")
                }
            }

            // Create session
            Log.d(TAG, "Creating ARCore session...")
            session = Session(this)
            
            // Configure session
            session?.configure(Config(session).apply {
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                planeFindingMode = Config.PlaneFindingMode.DISABLED
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            })
            
            Log.d(TAG, "✅ ARCore session created successfully")
            return true

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Camera permission denied: ${e.message}")
            showErrorAndFinish("Camera permission is required for AR")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Session creation failed", e)
            showErrorAndFinish("ARCore error: ${e.message}")
            return false
        }
    }

    // ═══════════════════════════════════════════════════════════
    // OPENGL RENDERING
    // ═══════════════════════════════════════════════════════════

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

            backgroundRenderer.createOnGlThread()
            pointCloudRenderer.createOnGlThread()

            textureSet = false
            Log.d(TAG, "✅ OpenGL initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ GL init error", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        Log.d(TAG, "📺 Viewport: ${width}x${height}")
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            val session = session ?: return
            val detector = yoloDetector ?: return
            
            totalFrames++
            updateFps()

            @Suppress("DEPRECATION")
            val rotation = windowManager.defaultDisplay.rotation
            displayRotation = when (rotation) {
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight)

            if (!textureSet) {
                session.setCameraTextureName(backgroundRenderer.getTextureId())
                textureSet = true
            }

            val frame = session.update()
            val camera = frame.camera

            backgroundRenderer.draw(frame)

            if (camera.trackingState != TrackingState.TRACKING) {
                mainHandler.post {
                    boxOverlayView.clear()
                    textOverlayView.clear()
                    updateStatus("📱 Move device to start tracking")
                }
                return
            }

            // Point cloud (optional)
            try {
                val projM = FloatArray(16)
                camera.getProjectionMatrix(projM, 0, 0.1f, 100f)
                val viewM = FloatArray(16)
                camera.getViewMatrix(viewM, 0)

                val cloud = frame.acquirePointCloud()
                pointCloudRenderer.update(cloud)
                pointCloudRenderer.draw(viewM, projM)
                cloud.release()
            } catch (e: Exception) {}

            // 🔥 DETECTION PIPELINE
            frameCounter++
            if (frameCounter % detectionInterval == 0 && !isDetecting) {
                runDetection(frame)
            }

            cleanupStaleTrackers()
            adjustDetectionInterval()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Frame error", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DETECTION PIPELINE (Same as before)
    // ═══════════════════════════════════════════════════════════
    
    private fun runDetection(frame: Frame) {
        if (isDetecting) return
        isDetecting = true
        detectionFrames++

        val startTime = System.currentTimeMillis()

        val image = try {
            frame.acquireCameraImage()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire camera image: ${e.message}")
            isDetecting = false
            return
        }

        cameraImageWidth = image.width
        cameraImageHeight = image.height

        detectionScope.launch {
            var bitmap: Bitmap? = null

            try {
                bitmap = imageToBitmap(image)
                if (bitmap == null) {
                    Log.e(TAG, "❌ Bitmap conversion failed")
                    return@launch
                }

                val yoloResults = try {
                    yoloDetector?.detect(bitmap) ?: emptyList()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ YOLO detection failed", e)
                    emptyList()
                }

                processDetections(yoloResults, bitmap.width, bitmap.height)

                val elapsed = System.currentTimeMillis() - startTime
                detectionTimes.addLast(elapsed)
                if (detectionTimes.size > 20) detectionTimes.removeFirst()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Detection pipeline error", e)
            } finally {
                try { image.close() } catch (_: Exception) {}
                bitmap?.let { if (!it.isRecycled) it.recycle() }
                isDetecting = false
            }
        }
    }

    @Synchronized
    private fun processDetections(
        detections: List<YoloResult>,
        imageWidth: Int,
        imageHeight: Int
    ) {
        val now = System.currentTimeMillis()
        val finalDetections = mutableListOf<DetectedObjectBox>()

        val byClass = detections.groupBy { it.label }

        for ((className, classDetections) in byClass) {
            for (det in classDetections) {
                if (det.confidence < MIN_CONFIDENCE) continue

                val screenRect = transformCoordinates(
                    det.box, imageWidth, imageHeight,
                    viewportWidth, viewportHeight, displayRotation
                )

                val w = screenRect.width()
                val h = screenRect.height()
                if (w < MIN_BOX_SIZE || h < MIN_BOX_SIZE) continue

                val aspectRatio = w.toFloat() / h.toFloat()
                if (aspectRatio < 0.1f || aspectRatio > 10f) continue

                val screenArea = viewportWidth * viewportHeight
                val boxArea = w * h
                if (boxArea > screenArea * MAX_BOX_RATIO || boxArea < 100) continue

                val trackerId = generateTrackerId(det.label, screenRect)

                val tracker = objectTrackers[trackerId]
                if (tracker != null) {
                    val smoothedRect = smoothRectTransition(tracker.smoothedRect, screenRect)
                    
                    tracker.apply {
                        this.rect = screenRect
                        this.smoothedRect = smoothedRect
                        this.lastSeen = now
                        this.detectionCount++
                        this.avgConfidence = (avgConfidence * 0.7f + det.confidence * 0.3f)
                        this.confidence = if (detectionCount >= 3) {
                            minOf(avgConfidence + CONFIDENCE_BOOST, 1.0f)
                        } else {
                            avgConfidence
                        }
                        this.isStable = detectionCount >= 3
                    }
                } else {
                    objectTrackers[trackerId] = ObjectTracker(
                        id = trackerId,
                        label = det.label,
                        confidence = det.confidence,
                        rect = screenRect,
                        smoothedRect = RectF(screenRect),
                        lastSeen = now,
                        detectionCount = 1,
                        avgConfidence = det.confidence,
                        isStable = false
                    )
                }
            }
        }

        for (tracker in objectTrackers.values) {
            finalDetections.add(
                DetectedObjectBox(
                    rect = Rect(
                        tracker.smoothedRect.left.toInt(),
                        tracker.smoothedRect.top.toInt(),
                        tracker.smoothedRect.right.toInt(),
                        tracker.smoothedRect.bottom.toInt()
                    ),
                    label = tracker.label,
                    confidence = tracker.confidence
                )
            )
        }

        if (finalDetections.isNotEmpty()) {
            lastDetectionTime = now
            consecutiveEmptyFrames = 0
        } else {
            consecutiveEmptyFrames++
        }

        mainHandler.post {
            boxOverlayView.updateBoxes(finalDetections)
            textOverlayView.updateBoxes(finalDetections)
            updateStatusWithResults(finalDetections)
        }
    }

    private fun transformCoordinates(
        normalizedBox: RectF,
        imageWidth: Int, imageHeight: Int,
        screenWidth: Int, screenHeight: Int,
        rotation: Int
    ): Rect {
        var left = normalizedBox.left * imageWidth
        var top = normalizedBox.top * imageHeight
        var right = normalizedBox.right * imageWidth
        var bottom = normalizedBox.bottom * imageHeight

        when (rotation) {
            90 -> {
                val temp = left
                left = top
                top = imageWidth - right
                right = bottom
                bottom = imageWidth - temp
            }
            180 -> {
                val tempL = left
                val tempT = top
                left = imageWidth - right
                top = imageHeight - bottom
                right = imageWidth - tempL
                bottom = imageHeight - tempT
            }
            270 -> {
                val newLeft = imageHeight - bottom
                val newTop = left
                val newRight = imageHeight - top
                val newBottom = right
                left = newLeft
                top = newTop
                right = newRight
                bottom = newBottom
            }
        }

        val scaleX = screenWidth.toFloat() / 
            if (rotation == 90 || rotation == 270) imageHeight else imageWidth
        val scaleY = screenHeight.toFloat() / 
            if (rotation == 90 || rotation == 270) imageWidth else imageHeight

        return Rect(
            (left * scaleX).toInt().coerceIn(0, screenWidth),
            (top * scaleY).toInt().coerceIn(0, screenHeight),
            (right * scaleX).toInt().coerceIn(0, screenWidth),
            (bottom * scaleY).toInt().coerceIn(0, screenHeight)
        )
    }

    private fun smoothRectTransition(prev: RectF, curr: Rect): RectF {
        return RectF(
            prev.left + (curr.left - prev.left) * SMOOTHING_FACTOR,
            prev.top + (curr.top - prev.top) * SMOOTHING_FACTOR,
            prev.right + (curr.right - prev.right) * SMOOTHING_FACTOR,
            prev.bottom + (curr.bottom - prev.bottom) * SMOOTHING_FACTOR
        )
    }

    private fun generateTrackerId(label: String, rect: Rect): String {
        val gridSize = 100
        val gridX = rect.centerX() / gridSize
        val gridY = rect.centerY() / gridSize
        return "${label}_${gridX}_${gridY}"
    }

    @Synchronized
    private fun cleanupStaleTrackers() {
        val now = System.currentTimeMillis()
        objectTrackers.entries.removeIf { (_, tracker) ->
            now - tracker.lastSeen > TRACKING_TIMEOUT_MS
        }
    }

    private fun adjustDetectionInterval() {
        detectionInterval = when {
            objectTrackers.size >= 5 -> 5
            objectTrackers.size >= 2 -> 4
            objectTrackers.isNotEmpty() -> 3
            else -> 4
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
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

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bitmap conversion error", e)
            null
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UI UPDATES
    // ═══════════════════════════════════════════════════════════

    private fun updateStatusWithResults(results: List<DetectedObjectBox>) {
        val avgTime = if (detectionTimes.isNotEmpty()) {
            detectionTimes.average().toLong()
        } else 0L

        val status = if (results.isEmpty()) {
            "FPS: $fps | ⏱ ${avgTime}ms | 🔍 Scanning..."
        } else {
            val tracked = objectTrackers.count { it.value.isStable }
            val summary = results.take(2).joinToString(", ") { 
                "${it.label} ${(it.confidence * 100).toInt()}%" 
            }
            val suffix = if (results.size > 2) " +${results.size - 2}" else ""
            "FPS: $fps | ⏱${avgTime}ms | ✅$summary$suffix | 🎯${tracked}/${results.size}"
        }
        updateStatus(status)
    }

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
        mainHandler.post { statusText.text = msg }
    }

    private fun showErrorAndFinish(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}