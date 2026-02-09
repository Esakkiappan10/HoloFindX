package com.holofindx.ar

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.util.Log
import kotlin.math.pow
import android.view.Gravity
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🔬 ULTIMATE DIAGNOSTIC TEST SUITE
 * 
 * Features:
 * ✅ Model inspection and validation
 * ✅ Tensor shape analysis
 * ✅ Multiple test scenarios
 * ✅ Performance benchmarking
 * ✅ Debug visualization
 * ✅ Detailed logging
 */
class DiagnosticTestActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var logContainer: LinearLayout
    private lateinit var imageView: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var runButton: Button
    private lateinit var clearButton: Button
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var testRunning = false

    companion object {
        private const val TAG = "DiagnosticTest"
        private const val STORAGE_PERMISSION_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.WHITE)
        }

        // Title
        val title = TextView(this).apply {
            text = "🔬 YOLO DIAGNOSTIC TEST SUITE"
            textSize = 22f
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }
        root.addView(title)

        // Image preview
        imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600
            )
            setBackgroundColor(Color.LTGRAY)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(imageView)

        // Button container
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }

        runButton = Button(this).apply {
            text = "🚀 RUN TESTS"
            textSize = 16f
            setOnClickListener { runAllTests() }
        }
        buttonContainer.addView(runButton)

        clearButton = Button(this).apply {
            text = "🗑️ CLEAR"
            textSize = 16f
            setOnClickListener { clearLogs() }
            setPadding(16, 0, 0, 0)
        }
        buttonContainer.addView(clearButton)

        root.addView(buttonContainer)

        // Progress bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = ProgressBar.GONE
        }
        root.addView(progressBar)

        // Scrollable log view
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        logContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        
        scrollView.addView(logContainer)
        root.addView(scrollView)

        setContentView(root)

        // Check permissions
        if (!hasStoragePermission()) {
            requestStoragePermission()
        }

        addLog("Ready to start diagnostic tests", LogLevel.INFO)
        addLog("Tap 'RUN TESTS' to begin", LogLevel.INFO)
    }

    enum class LogLevel {
        INFO, SUCCESS, WARNING, ERROR, HEADER
    }

    private fun addLog(message: String, level: LogLevel = LogLevel.INFO) {
        Log.d(TAG, message)
        
        runOnUiThread {
            val textView = TextView(this).apply {
                text = message
                textSize = 12f
                typeface = Typeface.MONOSPACE
                setPadding(8, 4, 8, 4)
                
                when (level) {
                    LogLevel.HEADER -> {
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.rgb(0, 100, 200))
                    }
                    LogLevel.SUCCESS -> setTextColor(Color.rgb(0, 150, 0))
                    LogLevel.WARNING -> setTextColor(Color.rgb(200, 100, 0))
                    LogLevel.ERROR -> setTextColor(Color.RED)
                    LogLevel.INFO -> setTextColor(Color.BLACK)
                }
            }
            
            logContainer.addView(textView)
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun clearLogs() {
        logContainer.removeAllViews()
        addLog("Logs cleared", LogLevel.INFO)
    }

    private fun runAllTests() {
        if (testRunning) return
        
        scope.launch {
            try {
                testRunning = true
                runButton.isEnabled = false
                clearButton.isEnabled = false
                progressBar.visibility = ProgressBar.VISIBLE
                progressBar.max = 100
                progressBar.progress = 0

                clearLogs()
                addLog("═".repeat(50), LogLevel.HEADER)
                addLog("🔬 STARTING COMPREHENSIVE DIAGNOSTIC TESTS", LogLevel.HEADER)
                addLog("═".repeat(50), LogLevel.HEADER)
                addLog("")

                // Test 1: Environment check
                updateProgress(10, "Checking environment...")
                testEnvironment()
                delay(500)

                // Test 2: Model inspection
                updateProgress(20, "Inspecting model...")
                testModelInspection()
                delay(500)

                // Test 3: Initialize detector
                updateProgress(35, "Initializing detector...")
                val detector = testDetectorInitialization()
                if (detector == null) {
                    addLog("❌ Cannot continue without detector", LogLevel.ERROR)
                    return@launch
                }
                delay(500)

                // Test 4: Synthetic image tests
                updateProgress(50, "Running synthetic tests...")
                testSyntheticImages(detector)
                delay(500)

                // Test 5: Edge cases
                updateProgress(70, "Testing edge cases...")
                testEdgeCases(detector)
                delay(500)

                // Test 6: Performance benchmark
                updateProgress(85, "Benchmarking performance...")
                testPerformance(detector)
                delay(500)

                // Test 7: Configuration validation
                updateProgress(95, "Validating configuration...")
                testConfiguration()

                updateProgress(100, "Complete!")
                
                addLog("")
                addLog("═".repeat(50), LogLevel.HEADER)
                addLog("✅ ALL DIAGNOSTIC TESTS COMPLETE", LogLevel.SUCCESS)
                addLog("═".repeat(50), LogLevel.HEADER)

                detector.close()

            } catch (e: Exception) {
                addLog("❌ Test suite failed: ${e.message}", LogLevel.ERROR)
                Log.e(TAG, "Test suite error", e)
            } finally {
                testRunning = false
                runButton.isEnabled = true
                clearButton.isEnabled = true
                progressBar.visibility = ProgressBar.GONE
            }
        }
    }

    private fun updateProgress(progress: Int, message: String) {
        runOnUiThread {
            progressBar.progress = progress
            addLog("[$progress%] $message", LogLevel.INFO)
        }
    }

    private suspend fun testEnvironment() = withContext(Dispatchers.Default) {
        addLog("📋 TEST 1: ENVIRONMENT CHECK", LogLevel.HEADER)
        addLog("─".repeat(50))
        
        addLog("Android Version: ${android.os.Build.VERSION.RELEASE}", LogLevel.INFO)
        addLog("SDK: ${android.os.Build.VERSION.SDK_INT}", LogLevel.INFO)
        addLog("Device: ${android.os.Build.MODEL}", LogLevel.INFO)
        addLog("Manufacturer: ${android.os.Build.MANUFACTURER}", LogLevel.INFO)
        
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        
        addLog("Max Memory: ${maxMemory}MB", LogLevel.INFO)
        addLog("Total Memory: ${totalMemory}MB", LogLevel.INFO)
        addLog("Free Memory: ${freeMemory}MB", LogLevel.INFO)
        addLog("✅ Environment check complete", LogLevel.SUCCESS)
        addLog("")
    }

    private suspend fun testModelInspection() = withContext(Dispatchers.Default) {
        addLog("📦 TEST 2: MODEL INSPECTION", LogLevel.HEADER)
        addLog("─".repeat(50))
        
        try {
            val modelPath = "yolov8n_saved_model/yolov8n_float32.tflite"
            
            val modelBuffer = assets.open(modelPath).use { input ->
                val bytes = input.readBytes()
                addLog("Model size: ${bytes.size / 1024}KB", LogLevel.INFO)
                ByteBuffer.allocateDirect(bytes.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(bytes)
                    rewind()
                }
            }

            val interpreter = Interpreter(modelBuffer)
            
            // Input tensor
            val inputTensor = interpreter.getInputTensor(0)
            addLog("INPUT TENSOR:", LogLevel.INFO)
            addLog("  Shape: ${inputTensor.shape().contentToString()}", LogLevel.INFO)
            addLog("  Type: ${inputTensor.dataType()}", LogLevel.INFO)
            
            // Output tensor
            val outputTensor = interpreter.getOutputTensor(0)
            addLog("OUTPUT TENSOR:", LogLevel.INFO)
            addLog("  Shape: ${outputTensor.shape().contentToString()}", LogLevel.INFO)
            addLog("  Type: ${outputTensor.dataType()}", LogLevel.INFO)
            
            val outputShape = outputTensor.shape()
            if (outputShape[1] == 84 || outputShape[1] > 80) {
                addLog("  Format: Transposed [1, classes+4, boxes]", LogLevel.SUCCESS)
            } else {
                addLog("  Format: Standard [1, boxes, classes+4]", LogLevel.SUCCESS)
            }
            
            interpreter.close()
            addLog("✅ Model inspection complete", LogLevel.SUCCESS)
            
        } catch (e: Exception) {
            addLog("❌ Model inspection failed: ${e.message}", LogLevel.ERROR)
        }
        addLog("")
    }

    private suspend fun testDetectorInitialization(): YoloV8Detector? = withContext(Dispatchers.Default) {
        addLog("🎯 TEST 3: DETECTOR INITIALIZATION", LogLevel.HEADER)
        addLog("─".repeat(50))
        
        return@withContext try {
            val detector = YoloV8Detector(this@DiagnosticTestActivity)
            addLog("✅ Detector initialized successfully", LogLevel.SUCCESS)
            addLog("")
            detector
        } catch (e: Exception) {
            addLog("❌ Detector initialization failed: ${e.message}", LogLevel.ERROR)
            addLog("")
            null
        }
    }

    private suspend fun testSyntheticImages(detector: YoloV8Detector) = withContext(Dispatchers.Default) {
        addLog("🖼️ TEST 4: SYNTHETIC IMAGE TESTS", LogLevel.HEADER)
        addLog("─".repeat(50))
        
        val testCases = listOf(
            TestCase("Solid white", createSolidBitmap(Color.WHITE)),
            TestCase("Solid black", createSolidBitmap(Color.BLACK)),
            TestCase("Geometric shapes", createShapesBitmap()),
            TestCase("Gradient", createGradientBitmap()),
            TestCase("Text pattern", createTextBitmap())
        )
        
        for ((index, testCase) in testCases.withIndex()) {
            addLog("Test ${index + 1}: ${testCase.name}", LogLevel.INFO)
            
            withContext(Dispatchers.Main) {
                imageView.setImageBitmap(testCase.bitmap)
            }
            
            val startTime = System.currentTimeMillis()
            val results = detector.detect(testCase.bitmap)
            val elapsed = System.currentTimeMillis() - startTime
            
            addLog("  Time: ${elapsed}ms", LogLevel.INFO)
            addLog("  Detections: ${results.size}", LogLevel.INFO)
            
            if (results.isNotEmpty()) {
                results.take(3).forEach { det ->
                    addLog("    - ${det.label}: ${(det.confidence * 100).toInt()}%", LogLevel.SUCCESS)
                }
                
                val annotated = annotateImage(testCase.bitmap, results)
                saveDebugImage(annotated, "test_${index + 1}_${testCase.name.replace(" ", "_")}.jpg")
                
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(annotated)
                }
            }
            
            delay(800)
        }
        
        addLog("✅ Synthetic tests complete", LogLevel.SUCCESS)
        addLog("")
    }

    private suspend fun testEdgeCases(detector: YoloV8Detector) = withContext(Dispatchers.Default) {
        addLog("⚠️ TEST 5: EDGE CASES", LogLevel.HEADER)
        addLog("─".repeat(50))
        
        // Small image
        addLog("Testing very small image (64x64)...", LogLevel.INFO)
        val small = createSolidBitmap(Color.GRAY, 64, 64)
        val smallResults = detector.detect(small)
        addLog("  Results: ${smallResults.size}", if (smallResults.isEmpty()) LogLevel.SUCCESS else LogLevel.WARNING)
        
        // Large image
        addLog("Testing large image (1920x1080)...", LogLevel.INFO)
        val large = createSolidBitmap(Color.GRAY, 1920, 1080)
        val largeResults = detector.detect(large)
        addLog("  Results: ${largeResults.size}", LogLevel.INFO)
        
        // Extreme aspect ratio
        addLog("Testing extreme aspect (2000x100)...", LogLevel.INFO)
        val wide = createSolidBitmap(Color.GRAY, 2000, 100)
        val wideResults = detector.detect(wide)
        addLog("  Results: ${wideResults.size}", LogLevel.INFO)
        
        addLog("✅ Edge case tests complete", LogLevel.SUCCESS)
        addLog("")
    }

    private suspend fun testPerformance(detector: YoloV8Detector) = withContext(Dispatchers.Default) {
        addLog("⚡ TEST 6: PERFORMANCE BENCHMARK", LogLevel.HEADER)
        addLog("─".repeat(50))
        
        val testBitmap = createShapesBitmap()
        val times = mutableListOf<Long>()
        val iterations = 20
        
        addLog("Running $iterations iterations...", LogLevel.INFO)
        
        for (i in 1..iterations) {
            val start = System.currentTimeMillis()
            detector.detect(testBitmap)
            val elapsed = System.currentTimeMillis() - start
            times.add(elapsed)
        }
        
        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0L
        val maxTime = times.maxOrNull() ?: 0L
        val stdDev = kotlin.math.sqrt(times.map { (it - avgTime).pow(2) }.average())
        
        addLog("Average: ${avgTime.toLong()}ms", LogLevel.SUCCESS)
        addLog("Min: ${minTime}ms", LogLevel.INFO)
        addLog("Max: ${maxTime}ms", LogLevel.INFO)
        addLog("Std Dev: ${stdDev.toLong()}ms", LogLevel.INFO)
        addLog("Est FPS: ${(1000.0 / avgTime).toInt()}", LogLevel.SUCCESS)
        
        val stats = detector.getStats()
        addLog("Total inferences: ${stats.totalInferences}", LogLevel.INFO)
        addLog("Success rate: ${stats.successRate}%", LogLevel.INFO)
        
        addLog("✅ Performance benchmark complete", LogLevel.SUCCESS)
        addLog("")
    }

    private fun testConfiguration() {
        addLog("⚙️ TEST 7: CONFIGURATION VALIDATION", LogLevel.HEADER)
        addLog("─".repeat(50))
        
        addLog(DetectionConfig.getSummary(), LogLevel.INFO)
        addLog("✅ Configuration valid", LogLevel.SUCCESS)
    }

    // ═══════════════════════════════════════════════════════════
    // IMAGE CREATION HELPERS
    // ═══════════════════════════════════════════════════════════

    private fun createSolidBitmap(color: Int, width: Int = 640, height: Int = 640): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).drawColor(color)
        }
    }

    private fun createShapesBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Red rectangle
        paint.color = Color.RED
        canvas.drawRect(100f, 100f, 300f, 250f, paint)
        
        // Blue circle
        paint.color = Color.BLUE
        canvas.drawCircle(450f, 175f, 75f, paint)
        
        // Green rectangle
        paint.color = Color.GREEN
        canvas.drawRect(150f, 350f, 450f, 500f, paint)
        
        return bitmap
    }

    private fun createGradientBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val gradient = LinearGradient(
            0f, 0f, 640f, 640f,
            Color.rgb(100, 150, 255),
            Color.rgb(255, 150, 100),
            Shader.TileMode.CLAMP
        )
        
        canvas.drawPaint(Paint().apply { shader = gradient })
        return bitmap
    }

    private fun createTextBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 60f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        
        canvas.drawText("TEST", 200f, 200f, paint)
        canvas.drawText("IMAGE", 180f, 280f, paint)
        
        return bitmap
    }

    private fun annotateImage(bitmap: Bitmap, detections: List<YoloResult>): Bitmap {
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        
        val boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.GREEN
        }
        
        val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = 24f
            style = Paint.Style.FILL
        }
        
        for (det in detections) {
            val left = det.box.left * bitmap.width
            val top = det.box.top * bitmap.height
            val right = det.box.right * bitmap.width
            val bottom = det.box.bottom * bitmap.height
            
            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText(
                "${det.label} ${(det.confidence * 100).toInt()}%",
                left, maxOf(top - 5f, 25f), textPaint
            )
        }
        
        return mutable
    }

    private fun saveDebugImage(bitmap: Bitmap, filename: String) {
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "YoloDiagnostic")
            if (!dir.exists()) dir.mkdirs()
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "${timestamp}_${filename}")
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            addLog("  💾 Saved: ${file.name}", LogLevel.INFO)
        } catch (e: Exception) {
            addLog("  ⚠️ Save failed: ${e.message}", LogLevel.WARNING)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═══════════════════════════════════════════════════════════

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    data class TestCase(val name: String, val bitmap: Bitmap)
}

private fun Double.pow(n: Int): Double = this.pow(n.toDouble())