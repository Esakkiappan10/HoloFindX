package com.holofindx.ar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 🔬 YOLO DIAGNOSTIC TEST ACTIVITY
 * 
 * This activity will:
 * 1. Test if TFLite model loads correctly
 * 2. Check input/output tensor shapes
 * 3. Generate a test image with known objects
 * 4. Run inference and show detailed results
 * 5. Save debug images to help diagnose issues
 */
class YoloDiagnosticActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var imageView: ImageView
    private lateinit var runTestButton: Button
    private lateinit var yoloDetector: YoloV8Detector
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val logs = mutableListOf<String>()

    companion object {
        private const val TAG = "YoloDiagnostic"
        private const val STORAGE_PERMISSION_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        // Title
        val title = TextView(this).apply {
            text = "🔬 YOLOv8 Diagnostic Test"
            textSize = 20f
            setTextColor(Color.BLACK)
            setPadding(0, 20, 0, 20)
        }
        layout.addView(title)

        // Image preview
        imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(800, 800)
            setBackgroundColor(Color.LTGRAY)
        }
        layout.addView(imageView)

        // Run test button
        runTestButton = Button(this).apply {
            text = "🚀 Run Diagnostic Test"
            setOnClickListener { runDiagnosticTest() }
        }
        layout.addView(runTestButton)

        // Scrollable log view
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        logTextView = TextView(this).apply {
            text = "Press button to start diagnostic test...\n"
            textSize = 12f
            setTextColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            setPadding(10, 10, 10, 10)
        }
        scrollView.addView(logTextView)
        layout.addView(scrollView)

        setContentView(layout)

        // Check permissions
        if (!hasStoragePermission()) {
            requestStoragePermission()
        }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        logs.add(message)
        runOnUiThread {
            logTextView.text = logs.joinToString("\n")
        }
    }

    private fun runDiagnosticTest() {
        scope.launch {
            try {
                runTestButton.isEnabled = false
                logs.clear()
                
                val separator = "=".repeat(50)
                log(separator)
                log("🔬 STARTING YOLO DIAGNOSTIC TEST")
                log(separator)
                
                // Step 1: Model inspection
                log("\n📋 STEP 1: Inspecting TFLite Model...")
                inspectModel()
                
                // Step 2: Initialize detector
                log("\n📋 STEP 2: Initializing YOLOv8 Detector...")
                yoloDetector = YoloV8Detector(this@YoloDiagnosticActivity)
                log("✅ Detector initialized successfully")
                
                // Step 3: Create test images
                log("\n📋 STEP 3: Creating Test Images...")
                val testImages = createTestImages()
                
                // Step 4: Run detection on each test image
                log("\n📋 STEP 4: Running Detection Tests...")
                for ((index, testImage) in testImages.withIndex()) {
                    log("\n--- Test Image ${index + 1} ---")
                    runDetectionTest(testImage.bitmap, testImage.description)
                }
                
                log("\n$separator")
                log("✅ DIAGNOSTIC TEST COMPLETE")
                log(separator)
                
            } catch (e: Exception) {
                log("\n❌ ERROR: ${e.message}")
                log("Stack trace: ${e.stackTraceToString()}")
            } finally {
                runTestButton.isEnabled = true
            }
        }
    }

    private fun inspectModel() {
        try {
            val modelPath = "yolov8n_saved_model/yolov8n_float32.tflite"
            
            val modelBuffer = assets.open(modelPath).use { input ->
                val bytes = input.readBytes()
                log("📦 Model file size: ${bytes.size / 1024} KB")
                ByteBuffer.allocateDirect(bytes.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(bytes)
                    rewind()
                }
            }

            val interpreter = Interpreter(modelBuffer)
            
            // Input tensor info
            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            val inputType = inputTensor.dataType()
            
            log("📥 INPUT TENSOR:")
            log("  Shape: ${inputShape.contentToString()}")
            log("  Type: $inputType")
            log("  Name: ${inputTensor.name()}")
            
            // Output tensor info
            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputType = outputTensor.dataType()
            
            log("📤 OUTPUT TENSOR:")
            log("  Shape: ${outputShape.contentToString()}")
            log("  Type: $outputType")
            log("  Name: ${outputTensor.name()}")
            
            // Determine format
            if (outputShape.size == 3) {
                if (outputShape[1] == 84 || outputShape[1] > 80) {
                    log("  Format: Transposed [1, classes+4, boxes]")
                } else {
                    log("  Format: Standard [1, boxes, classes+4]")
                }
            }
            
            interpreter.close()
            log("✅ Model inspection complete")
            
        } catch (e: Exception) {
            log("❌ Model inspection failed: ${e.message}")
        }
    }

    data class TestImage(val bitmap: Bitmap, val description: String)

    private fun createTestImages(): List<TestImage> {
        val images = mutableListOf<TestImage>()
        
        // Test 1: Simple shapes on white background
        images.add(TestImage(
            createSimpleShapesBitmap(),
            "Simple geometric shapes (high contrast)"
        ))
        
        // Test 2: Gradient background
        images.add(TestImage(
            createGradientBitmap(),
            "Gradient with shapes (medium contrast)"
        ))
        
        // Test 3: All white (edge case)
        images.add(TestImage(
            createSolidColorBitmap(Color.WHITE),
            "Solid white (edge case)"
        ))
        
        // Test 4: All black (edge case)
        images.add(TestImage(
            createSolidColorBitmap(Color.BLACK),
            "Solid black (edge case)"
        ))
        
        return images
    }

    private fun createSimpleShapesBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // White background
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Red rectangle (could be detected as "car" or "truck")
        paint.color = Color.RED
        canvas.drawRect(100f, 100f, 300f, 250f, paint)
        
        // Blue circle (could be detected as "ball" or "frisbee")
        paint.color = Color.BLUE
        canvas.drawCircle(450f, 175f, 75f, paint)
        
        // Green rectangle (could be detected as "book")
        paint.color = Color.GREEN
        canvas.drawRect(150f, 350f, 450f, 500f, paint)
        
        // Black text
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            isAntiAlias = true
        }
        canvas.drawText("TEST IMAGE", 200f, 600f, textPaint)
        
        return bitmap
    }

    private fun createGradientBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Gradient background
        val gradient = LinearGradient(
            0f, 0f, 640f, 640f,
            Color.rgb(200, 200, 255),
            Color.rgb(255, 200, 200),
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, 640f, 640f, paint)
        
        // Add some shapes
        val shapePaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(100, 100, 100)
        }
        canvas.drawRect(200f, 200f, 400f, 400f, shapePaint)
        
        return bitmap
    }

    private fun createSolidColorBitmap(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)
        return bitmap
    }

    private suspend fun runDetectionTest(bitmap: Bitmap, description: String) = withContext(Dispatchers.Default) {
        log("  Image: $description")
        log("  Size: ${bitmap.width}x${bitmap.height}")
        
        // Show image on UI
        withContext(Dispatchers.Main) {
            imageView.setImageBitmap(bitmap)
        }
        
        // Run detection
        val startTime = System.currentTimeMillis()
        val results = try {
            yoloDetector.detect(bitmap)
        } catch (e: Exception) {
            log("  ❌ Detection failed: ${e.message}")
            return@withContext
        }
        val detectionTime = System.currentTimeMillis() - startTime
        
        log("  ⏱️ Detection time: ${detectionTime}ms")
        log("  📦 Raw detections: ${results.size}")
        
        if (results.isEmpty()) {
            log("  ⚠️ NO DETECTIONS FOUND")
            log("  💡 This could mean:")
            log("     - Model is working but nothing detected (normal for test images)")
            log("     - Confidence threshold is too high")
            log("     - Model preprocessing is incorrect")
        } else {
            log("  ✅ DETECTIONS FOUND:")
            results.forEachIndexed { index, detection ->
                log("    [$index] ${detection.label}")
                log("        Confidence: ${(detection.confidence * 100).toInt()}%")
                log("        Box: [${detection.box.left}, ${detection.box.top}, ${detection.box.right}, ${detection.box.bottom}]")
                log("        Size: ${(detection.box.right - detection.box.left) * 640}x${(detection.box.bottom - detection.box.top) * 640} pixels")
            }
            
            // Draw detections on image
            val annotatedBitmap = drawDetectionsOnBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, true), results)
            withContext(Dispatchers.Main) {
                imageView.setImageBitmap(annotatedBitmap)
            }
            
            // Save annotated image
            saveImage(annotatedBitmap, "detection_result_${System.currentTimeMillis()}.jpg")
        }
        
        delay(1000) // Pause between tests
    }

    private fun drawDetectionsOnBitmap(bitmap: Bitmap, detections: List<YoloResult>): Bitmap {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.GREEN
        }
        
        val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = 30f
            style = Paint.Style.FILL
        }
        
        for (detection in detections) {
            val left = detection.box.left * bitmap.width
            val top = detection.box.top * bitmap.height
            val right = detection.box.right * bitmap.width
            val bottom = detection.box.bottom * bitmap.height
            
            canvas.drawRect(left, top, right, bottom, paint)
            canvas.drawText(
                "${detection.label} ${(detection.confidence * 100).toInt()}%",
                left,
                top - 10f,
                textPaint
            )
        }
        
        return bitmap
    }

    private fun saveImage(bitmap: Bitmap, filename: String) {
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "YoloDiagnostic")
            if (!dir.exists()) dir.mkdirs()
            
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            log("  💾 Saved: ${file.absolutePath}")
        } catch (e: Exception) {
            log("  ⚠️ Failed to save image: ${e.message}")
        }
    }

    // Permission handling
    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        if (::yoloDetector.isInitialized) {
            yoloDetector.close()
        }
    }
}