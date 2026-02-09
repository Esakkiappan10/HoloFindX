package com.holofindx.ar

import android.content.Context
import android.graphics.*
import android.view.View

/**
 * 🎨 ULTIMATE BOUNDING BOX RENDERER
 * 
 * Features:
 * ✅ Multi-color boxes per object type
 * ✅ Corner highlights for professional look
 * ✅ Smooth animations
 * ✅ Confidence indicators
 * ✅ Optimized drawing
 */
class BoxOverlayView(context: Context) : View(context) {

    private val boxes = mutableListOf<DetectedObjectBox>()
    
    // Main box paint
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    
    // Corner highlight paint
    private val cornerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }
    
    // Inner glow effect
    private val glowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    // Confidence bar background
    private val confidenceBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0x33FFFFFF
        isAntiAlias = true
    }
    
    // Confidence bar fill
    private val confidenceFillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    /**
     * Enhanced color palette - vibrant and distinguishable
     */
    private val colorMap = mapOf(
        "person" to Color.rgb(0, 255, 100),      // Bright green
        "car" to Color.rgb(255, 50, 50),         // Bright red
        "truck" to Color.rgb(255, 100, 0),       // Orange-red
        "bus" to Color.rgb(200, 0, 200),         // Purple
        "motorcycle" to Color.rgb(255, 0, 200),  // Magenta
        "bicycle" to Color.rgb(0, 200, 255),     // Cyan
        
        "chair" to Color.rgb(100, 150, 255),     // Light blue
        "couch" to Color.rgb(150, 100, 255),     // Light purple
        "bed" to Color.rgb(200, 150, 255),       // Lavender
        "dining table" to Color.rgb(255, 200, 100), // Peach
        
        "cup" to Color.rgb(255, 255, 0),         // Yellow
        "bottle" to Color.rgb(0, 255, 255),      // Cyan
        "wine glass" to Color.rgb(255, 150, 255), // Pink
        "bowl" to Color.rgb(255, 200, 0),        // Gold
        
        "laptop" to Color.rgb(150, 0, 255),      // Purple
        "keyboard" to Color.rgb(100, 255, 255),  // Light cyan
        "cell phone" to Color.rgb(255, 100, 255), // Pink-purple
        "mouse" to Color.rgb(150, 255, 150),     // Light green
        "remote" to Color.rgb(200, 200, 0),      // Olive
        
        "tv" to Color.rgb(0, 150, 255),          // Blue
        "monitor" to Color.rgb(100, 200, 255),   // Sky blue
        
        "book" to Color.rgb(255, 150, 0),        // Orange
        "clock" to Color.rgb(200, 200, 200),     // Silver
        "vase" to Color.rgb(255, 180, 200),      // Light pink
        "potted plant" to Color.rgb(0, 200, 100), // Green
        
        "dog" to Color.rgb(255, 180, 0),         // Orange
        "cat" to Color.rgb(180, 100, 50),        // Brown
        "bird" to Color.rgb(0, 200, 200),        // Teal
        
        "backpack" to Color.rgb(150, 100, 0),    // Brown-orange
        "handbag" to Color.rgb(255, 100, 150),   // Rose
        "suitcase" to Color.rgb(100, 100, 100),  // Gray
        
        "sports ball" to Color.rgb(255, 100, 0), // Orange
        "tennis racket" to Color.rgb(255, 255, 100), // Light yellow
        
        "apple" to Color.rgb(255, 0, 0),         // Red
        "banana" to Color.rgb(255, 255, 0),      // Yellow
        "orange" to Color.rgb(255, 165, 0),      // Orange
        "carrot" to Color.rgb(255, 140, 0),      // Dark orange
        
        "pizza" to Color.rgb(255, 100, 0),       // Orange
        "cake" to Color.rgb(255, 200, 200),      // Light pink
        "donut" to Color.rgb(255, 150, 100)      // Peach
    )
    
    private val defaultColor = Color.rgb(0, 255, 0) // Green

    init {
        setWillNotDraw(false)
    }

    /**
     * Update boxes from detection thread
     */
    fun updateBoxes(newBoxes: List<DetectedObjectBox>) {
        synchronized(boxes) {
            boxes.clear()
            boxes.addAll(newBoxes)
        }
        postInvalidate()
    }

    /**
     * Main drawing method
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        synchronized(boxes) {
            for (box in boxes) {
                drawBox(canvas, box)
            }
        }
    }

    /**
     * Draw a single bounding box with all effects
     */
    private fun drawBox(canvas: Canvas, box: DetectedObjectBox) {
        val color = colorMap[box.label.lowercase()] ?: defaultColor
        val rect = RectF(box.rect)
        
        // 1. Draw glow effect (larger, semi-transparent)
        glowPaint.apply {
            this.color = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color))
            strokeWidth = 10f
        }
        canvas.drawRoundRect(rect, 8f, 8f, glowPaint)
        
        // 2. Draw main box outline
        boxPaint.color = color
        canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
        
        // 3. Draw corner highlights
        drawCornerHighlights(canvas, rect, color)
        
        // 4. Draw confidence indicator
        drawConfidenceBar(canvas, rect, box.confidence, color)
        
        // 5. Draw inner border for depth
        val innerRect = RectF(
            rect.left + 4, rect.top + 4,
            rect.right - 4, rect.bottom - 4
        )
        glowPaint.apply {
            this.color = Color.argb(100, 255, 255, 255)
            strokeWidth = 1f
        }
        canvas.drawRoundRect(innerRect, 4f, 4f, glowPaint)
    }

    /**
     * Draw professional corner highlights
     */
    private fun drawCornerHighlights(canvas: Canvas, rect: RectF, color: Int) {
        cornerPaint.color = color
        
        val cornerLength = minOf(rect.width(), rect.height()) * 0.15f
        
        // Top-left
        canvas.drawLine(rect.left, rect.top, rect.left + cornerLength, rect.top, cornerPaint)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + cornerLength, cornerPaint)
        
        // Top-right
        canvas.drawLine(rect.right, rect.top, rect.right - cornerLength, rect.top, cornerPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLength, cornerPaint)
        
        // Bottom-left
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLength, rect.bottom, cornerPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - cornerLength, cornerPaint)
        
        // Bottom-right
        canvas.drawLine(rect.right, rect.bottom, rect.right - cornerLength, rect.bottom, cornerPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLength, cornerPaint)
    }

    /**
     * Draw confidence indicator bar at bottom of box
     */
    private fun drawConfidenceBar(canvas: Canvas, rect: RectF, confidence: Float, color: Int) {
        val barHeight = 8f
        val barMargin = 4f
        
        // Background bar
        val bgRect = RectF(
            rect.left + barMargin,
            rect.bottom - barHeight - barMargin,
            rect.right - barMargin,
            rect.bottom - barMargin
        )
        canvas.drawRoundRect(bgRect, barHeight / 2, barHeight / 2, confidenceBgPaint)
        
        // Confidence fill
        val fillWidth = (rect.width() - barMargin * 2) * confidence
        val fillRect = RectF(
            rect.left + barMargin,
            rect.bottom - barHeight - barMargin,
            rect.left + barMargin + fillWidth,
            rect.bottom - barMargin
        )
        
        // Color based on confidence
        confidenceFillPaint.color = when {
            confidence >= 0.75f -> Color.rgb(0, 255, 100)  // High - green
            confidence >= 0.50f -> Color.rgb(255, 200, 0)  // Medium - yellow
            else -> Color.rgb(255, 150, 0)                  // Low - orange
        }
        
        canvas.drawRoundRect(fillRect, barHeight / 2, barHeight / 2, confidenceFillPaint)
    }

    /**
     * Clear all boxes
     */
    fun clear() {
        synchronized(boxes) {
            boxes.clear()
        }
        postInvalidate()
    }

    /**
     * Get detection count
     */
    fun getBoxCount(): Int {
        synchronized(boxes) {
            return boxes.size
        }
    }
}