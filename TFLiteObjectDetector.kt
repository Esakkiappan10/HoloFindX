package com.holofindx.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite Object Detector - CPU ONLY (No GPU dependency)
 * 100% Compatible - Works on all devices
 */
class TFLiteObjectDetector(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()
    private var isInitialized = false

    companion object {
        private const val TAG = "TFLiteDetector"
        
        private const val MODEL_FILE = "detect.tflite"
        private const val LABELS_FILE = "labelmap.txt"
        
        private const val INPUT_SIZE = 300
        private const val PIXEL_SIZE = 3
        private const val NUM_DETECTIONS = 10
        
        private const val MIN_CONFIDENCE = 0.5f
    }

    data class Detection(
        val rect: Rect,
        val label: String,
        val confidence: Float
    )

    /**
     * Initialize detector - CPU only, no GPU
     */
    fun initialize() {
        try {
            Log.d(TAG, "Initializing TFLite (CPU only)...")
            
            loadLabels()
            
            val model = loadModelFile()
            
            // CPU-only configuration - NO GPU
            val options = Interpreter.Options().apply {
                setNumThreads(4) // Use 4 CPU threads
                setUseNNAPI(false) // Disable NNAPI
            }
            
            interpreter = Interpreter(model, options)
            isInitialized = true
            
            Log.d(TAG, "✅ TFLite initialized (CPU mode) with ${labels.size} classes")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize TFLite", e)
            isInitialized = false
            throw e
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "Detector not initialized")
            return emptyList()
        }

        try {
            val inputBuffer = preprocessImage(bitmap)
            
            val outputLocations = Array(1) { Array(NUM_DETECTIONS) { FloatArray(4) } }
            val outputClasses = Array(1) { FloatArray(NUM_DETECTIONS) }
            val outputScores = Array(1) { FloatArray(NUM_DETECTIONS) }
            val numDetections = FloatArray(1)
            
            val outputMap = mapOf(
                0 to outputLocations,
                1 to outputClasses,
                2 to outputScores,
                3 to numDetections
            )
            
            interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)
            
            val detections = mutableListOf<Detection>()
            val numFound = numDetections[0].toInt().coerceIn(0, NUM_DETECTIONS)
            
            for (i in 0 until numFound) {
                val confidence = outputScores[0][i]
                
                if (confidence >= MIN_CONFIDENCE) {
                    val classId = outputClasses[0][i].toInt()
                    val label = if (classId in labels.indices) labels[classId] else "Unknown"
                    
                    if (label.equals("background", ignoreCase = true)) continue
                    
                    val location = outputLocations[0][i]
                    val rect = Rect(
                        (location[1] * bitmap.width).toInt(),
                        (location[0] * bitmap.height).toInt(),
                        (location[3] * bitmap.width).toInt(),
                        (location[2] * bitmap.height).toInt()
                    )
                    
                    detections.add(Detection(rect, label, confidence))
                }
            }
            
            return detections
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Detection failed", e)
            return emptyList()
        }
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        val buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        buffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        for (pixel in pixels) {
            buffer.put(((pixel shr 16) and 0xFF).toByte())
            buffer.put(((pixel shr 8) and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
        }
        
        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(): MappedByteBuffer {
        try {
            val fileDescriptor = context.assets.openFd(MODEL_FILE)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load model: $MODEL_FILE", e)
            throw RuntimeException("Model file not found in assets folder", e)
        }
    }

    private fun loadLabels() {
        try {
            context.assets.open(LABELS_FILE).bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    labels.add(line.trim())
                }
            }
            Log.d(TAG, "✅ Loaded ${labels.size} labels")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Using default labels", e)
            labels.addAll(getDefaultLabels())
        }
    }

    private fun getDefaultLabels(): List<String> {
        return listOf(
            "background", "person", "bicycle", "car", "motorcycle", "airplane", "bus",
            "train", "truck", "boat", "traffic light", "fire hydrant", "street sign",
            "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse",
            "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "hat", "backpack",
            "umbrella", "shoe", "eye glasses", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove",
            "skateboard", "surfboard", "tennis racket", "bottle", "plate", "wine glass",
            "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake",
            "chair", "couch", "potted plant", "bed", "mirror", "dining table", "window",
            "desk", "toilet", "door", "tv", "laptop", "mouse", "remote", "keyboard",
            "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "blender",
            "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
        )
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            isInitialized = false
            Log.d(TAG, "✅ Detector closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing", e)
        }
    }
}