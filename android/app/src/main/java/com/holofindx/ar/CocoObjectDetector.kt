package com.holofindx.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class CocoDetection(
    val label: String,
    val confidence: Float,
    val box: RectF
)

class CocoObjectDetector(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputSize = 300

    init {
        // ✅ Correct: load model as ByteBuffer
        val modelBuffer = context.assets.open("detect.tflite").use {
            val bytes = it.readBytes()
            ByteBuffer.allocateDirect(bytes.size)
                .order(ByteOrder.nativeOrder())
                .put(bytes)
                .apply { rewind() }
        }

        interpreter = Interpreter(
            modelBuffer,
            Interpreter.Options().apply { setNumThreads(4) }
        )

        labels = context.assets.open("coco_labels.txt")
            .bufferedReader()
            .readLines()
    }

    fun detect(bitmap: Bitmap): List<CocoDetection> {

        // Resize to model input
        val scaled = Bitmap.createScaledBitmap(
            bitmap, inputSize, inputSize, true
        )

        // Input tensor
        val input = ByteBuffer.allocateDirect(
            1 * inputSize * inputSize * 3 * 4
        ).order(ByteOrder.nativeOrder())

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val px = scaled.getPixel(x, y)
                input.putFloat(((px shr 16) and 0xFF) / 255f)
                input.putFloat(((px shr 8) and 0xFF) / 255f)
                input.putFloat((px and 0xFF) / 255f)
            }
        }

        input.rewind() // ✅ VERY IMPORTANT

        // Output tensors
        val locations = Array(1) { Array(10) { FloatArray(4) } }
        val classes = Array(1) { FloatArray(10) }
        val scores = Array(1) { FloatArray(10) }
        val count = FloatArray(1)

        interpreter.runForMultipleInputsOutputs(
            arrayOf(input),
            mapOf(
                0 to locations,
                1 to classes,
                2 to scores,
                3 to count
            )
        )

        val results = mutableListOf<CocoDetection>()

        val numDetections = count[0].toInt().coerceAtMost(10)

        for (i in 0 until numDetections) {
            val score = scores[0][i]
            if (score < DetectionConfig.minConfidence) continue

            val classId = classes[0][i].toInt()
            if (classId !in labels.indices) continue

            val box = locations[0][i]

            results.add(
                CocoDetection(
                    label = labels[classId],
                    confidence = score,
                    box = RectF(
                        box[1], // left
                        box[0], // top
                        box[3], // right
                        box[2]  // bottom
                    )
                )
            )
        }

        return results
    }

    fun close() {
        interpreter.close()
    }
}
