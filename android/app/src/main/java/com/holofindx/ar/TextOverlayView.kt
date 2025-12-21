package com.holofindx.ar

import android.content.Context
import android.graphics.*
import android.view.View

/**
 * Professional text overlay renderer for object labels
 * Displays object names and confidence scores above bounding boxes
 * with color-coded backgrounds and smooth animations
 */
class TextOverlayView(context: Context) : View(context) {

    private val boxes = mutableListOf<DetectedObjectBox>()
    
    // Text paint with shadow for better readability
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
        isAntiAlias = true
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }
    
    // Background paint for label
    private val bgPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // Stroke paint for label border
    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
        color = Color.WHITE
    }
    
    /**
     * Color palette matching BoundingBoxRenderer
     * Each object type gets a unique color
     */
    private val colorMap = mapOf(
        "person" to Color.rgb(0, 255, 0),        // Green
        "car" to Color.rgb(255, 0, 0),           // Red
        "chair" to Color.rgb(0, 128, 255),       // Blue
        "cup" to Color.rgb(255, 255, 0),         // Yellow
        "laptop" to Color.rgb(255, 0, 255),      // Magenta
        "phone" to Color.rgb(0, 255, 255),       // Cyan
        "book" to Color.rgb(255, 128, 0),        // Orange
        "bottle" to Color.rgb(128, 255, 0),      // Lime
        "tv" to Color.rgb(255, 0, 128),          // Pink
        "keyboard" to Color.rgb(128, 0, 255),    // Purple
        "mouse" to Color.rgb(0, 255, 128),       // Mint
        "dog" to Color.rgb(255, 128, 0),         // Orange
        "cat" to Color.rgb(128, 64, 0),          // Brown
        "plant" to Color.rgb(0, 200, 0),         // Dark Green
        "clock" to Color.rgb(192, 192, 192),     // Silver
        "vase" to Color.rgb(255, 192, 203)       // Light Pink
    )
    
    private val defaultColor = Color.rgb(0, 255, 0) // Default green

    /**
     * Update the list of detected objects
     * Thread-safe method called from detection callback
     */
    fun updateBoxes(newBoxes: List<DetectedObjectBox>) {
        synchronized(boxes) {
            boxes.clear()
            boxes.addAll(newBoxes)
        }
        postInvalidate() // Request redraw
    }

    /**
     * Draw all labels on the canvas
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        synchronized(boxes) {
            for (box in boxes) {
                drawLabel(canvas, box)
            }
        }
    }

    /**
     * Draw a single label above a bounding box
     * @param canvas The canvas to draw on
     * @param box The detected object box
     */
    private fun drawLabel(canvas: Canvas, box: DetectedObjectBox) {
        // Format label text: "Person 85%"
        val labelText = "${capitalizeFirst(box.label)} ${(box.confidence * 100).toInt()}%"
        
        // Get color for this object type
        val boxColor = colorMap[box.label.lowercase()] ?: defaultColor
        
        // Measure text dimensions
        val textBounds = Rect()
        textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
        
        // Calculate label dimensions with padding
        val padding = 16f
        val labelWidth = textBounds.width() + padding * 2
        val labelHeight = textBounds.height() + padding * 2
        
        // Position label above the bounding box
        // If too close to top, position it inside the box instead
        val labelLeft = box.rect.left.toFloat()
        val labelTop = if (box.rect.top - labelHeight - 8 >= 0) {
            box.rect.top - labelHeight - 8
        } else {
            box.rect.top.toFloat() + 8
        }
        
        // Ensure label doesn't go off screen
        val adjustedLeft = labelLeft.coerceIn(0f, (width - labelWidth).coerceAtLeast(0f))
        
        // Create rounded rectangle for background
        val bgRect = RectF(
            adjustedLeft,
            labelTop,
            adjustedLeft + labelWidth,
            labelTop + labelHeight
        )
        
        // Draw background with semi-transparent color matching the box
        bgPaint.color = Color.argb(
            220, 
            Color.red(boxColor), 
            Color.green(boxColor), 
            Color.blue(boxColor)
        )
        canvas.drawRoundRect(bgRect, 8f, 8f, bgPaint)
        
        // Draw white stroke around label for contrast
        strokePaint.color = Color.WHITE
        canvas.drawRoundRect(bgRect, 8f, 8f, strokePaint)
        
        // Draw the text
        textPaint.color = Color.WHITE
        canvas.drawText(
            labelText,
            adjustedLeft + padding,
            labelTop + labelHeight - padding - 4f,
            textPaint
        )
        
        // Optional: Draw small confidence indicator
        drawConfidenceIndicator(canvas, box, bgRect, boxColor)
    }
    
    /**
     * Draw a small confidence indicator bar
     * Shows visual representation of detection confidence
     */
    private fun drawConfidenceIndicator(
        canvas: Canvas, 
        box: DetectedObjectBox, 
        labelRect: RectF,
        color: Int
    ) {
        val indicatorHeight = 4f
        val indicatorWidth = labelRect.width() * box.confidence
        
        val indicatorRect = RectF(
            labelRect.left,
            labelRect.bottom - indicatorHeight,
            labelRect.left + indicatorWidth,
            labelRect.bottom
        )
        
        val indicatorPaint = Paint().apply {
            this.color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawRect(indicatorRect, indicatorPaint)
    }
    
    /**
     * Capitalize first letter of string
     */
    private fun capitalizeFirst(str: String): String {
        return if (str.isEmpty()) {
            str
        } else {
            str.substring(0, 1).uppercase() + str.substring(1).lowercase()
        }
    }
    
    /**
     * Clear all labels from the view
     */
    fun clear() {
        synchronized(boxes) {
            boxes.clear()
        }
        postInvalidate()
    }
    
    /**
     * Get number of currently displayed labels
     */
    fun getDetectionCount(): Int {
        synchronized(boxes) {
            return boxes.size
        }
    }
    
    /**
     * Check if a specific object type is currently detected
     */
    fun isObjectDetected(label: String): Boolean {
        synchronized(boxes) {
            return boxes.any { it.label.equals(label, ignoreCase = true) }
        }
    }
    
    /**
     * Get all currently detected object labels
     */
    fun getDetectedLabels(): List<String> {
        synchronized(boxes) {
            return boxes.map { it.label }.distinct()
        }
    }
}