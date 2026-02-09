package com.holofindx.ar

/**
 * 🎯 ULTIMATE DETECTION CONFIGURATION
 * Centralized, production-ready configuration system
 */
object DetectionConfig {
    
    // ═══════════════════════════════════════════════════════════
    // DETECTION MODES
    // ═══════════════════════════════════════════════════════════
    
    enum class DetectionMode {
        PERFORMANCE,     // 30+ FPS, balanced accuracy
        BALANCED,        // 20-30 FPS, good accuracy (DEFAULT)
        QUALITY,         // 15-25 FPS, best accuracy
        BATTERY_SAVER    // 10-15 FPS, power efficient
    }
    
    var currentMode = DetectionMode.BALANCED
        private set
    
    // ═══════════════════════════════════════════════════════════
    // CORE DETECTION PARAMETERS
    // ═══════════════════════════════════════════════════════════
    
    var minConfidence: Float = 0.35f
        private set
    
    var iouThreshold: Float = 0.45f
        private set
    
    var minBoxSize: Int = 30
        private set
    
    var maxBoxRatio: Float = 0.90f
        private set
    
    var minAspectRatio: Float = 0.1f
        private set
    
    var maxAspectRatio: Float = 10.0f
        private set
    
    // ═══════════════════════════════════════════════════════════
    // FRAME PROCESSING
    // ═══════════════════════════════════════════════════════════
    
    var baseDetectionInterval: Int = 3
        private set
    
    var maxDetectionInterval: Int = 8
        private set
    
    var minDetectionInterval: Int = 2
        private set
    
    // ═══════════════════════════════════════════════════════════
    // TRACKING PARAMETERS
    // ═══════════════════════════════════════════════════════════
    
    var trackingTimeoutMs: Long = 2500L
        private set
    
    var smoothingFactor: Float = 0.35f
        private set
    
    var confidenceBoost: Float = 0.05f
        private set
    
    var stabilityFrames: Int = 3
        private set
    
    var maxTrackingDistance: Int = 150
        private set
    
    // ═══════════════════════════════════════════════════════════
    // PERFORMANCE SETTINGS
    // ═══════════════════════════════════════════════════════════
    
    var targetDetectionTimeMs: Long = 100L
        private set
    
    var maxDetectionTimeMs: Long = 200L
        private set
    
    var enableGpuAcceleration: Boolean = true
        private set
    
    var numThreads: Int = 4
        private set
    
    // ═══════════════════════════════════════════════════════════
    // VISUALIZATION
    // ═══════════════════════════════════════════════════════════
    
    var showBoundingBoxes: Boolean = true
        private set
    
    var showLabels: Boolean = true
        private set
    
    var showConfidence: Boolean = true
        private set
    
    var showFps: Boolean = true
        private set
    
    var boxStrokeWidth: Float = 6f
        private set
    
    var cornerHighlightLength: Float = 0.15f
        private set
    
    // ═══════════════════════════════════════════════════════════
    // MODE CONFIGURATIONS
    // ═══════════════════════════════════════════════════════════
    
    fun setMode(mode: DetectionMode) {
        currentMode = mode
        applyModeSettings()
    }
    
    private fun applyModeSettings() {
        when (currentMode) {
            DetectionMode.PERFORMANCE -> {
                minConfidence = 0.40f
                baseDetectionInterval = 4
                maxDetectionInterval = 10
                minDetectionInterval = 3
                trackingTimeoutMs = 2000L
                smoothingFactor = 0.40f
                stabilityFrames = 2
                targetDetectionTimeMs = 80L
            }
            
            DetectionMode.BALANCED -> {
                minConfidence = 0.35f
                baseDetectionInterval = 3
                maxDetectionInterval = 8
                minDetectionInterval = 2
                trackingTimeoutMs = 2500L
                smoothingFactor = 0.35f
                stabilityFrames = 3
                targetDetectionTimeMs = 100L
            }
            
            DetectionMode.QUALITY -> {
                minConfidence = 0.30f
                baseDetectionInterval = 2
                maxDetectionInterval = 6
                minDetectionInterval = 1
                trackingTimeoutMs = 3000L
                smoothingFactor = 0.30f
                stabilityFrames = 4
                targetDetectionTimeMs = 120L
            }
            
            DetectionMode.BATTERY_SAVER -> {
                minConfidence = 0.45f
                baseDetectionInterval = 6
                maxDetectionInterval = 12
                minDetectionInterval = 4
                trackingTimeoutMs = 2000L
                smoothingFactor = 0.40f
                stabilityFrames = 2
                targetDetectionTimeMs = 150L
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════
    // OBJECT-SPECIFIC SETTINGS
    // ═══════════════════════════════════════════════════════════
    
    fun getObjectSettings(label: String): ObjectSettings {
        return when (label.lowercase()) {
            "person" -> ObjectSettings(
                minConfidence = 0.40f,
                minSize = 50,
                preferredAspectRatio = 0.5f,
                priority = Priority.HIGH
            )
            
            "car", "truck", "bus" -> ObjectSettings(
                minConfidence = 0.45f,
                minSize = 60,
                preferredAspectRatio = 1.5f,
                priority = Priority.HIGH
            )
            
            "cell phone", "phone" -> ObjectSettings(
                minConfidence = 0.35f,
                minSize = 30,
                preferredAspectRatio = 0.6f,
                priority = Priority.MEDIUM
            )
            
            "laptop", "computer" -> ObjectSettings(
                minConfidence = 0.40f,
                minSize = 50,
                preferredAspectRatio = 1.4f,
                priority = Priority.MEDIUM
            )
            
            "cup", "bottle", "glass" -> ObjectSettings(
                minConfidence = 0.30f,
                minSize = 25,
                preferredAspectRatio = 0.4f,
                priority = Priority.LOW
            )
            
            "chair", "couch" -> ObjectSettings(
                minConfidence = 0.40f,
                minSize = 60,
                preferredAspectRatio = 1.0f,
                priority = Priority.MEDIUM
            )
            
            else -> ObjectSettings(
                minConfidence = this.minConfidence,
                minSize = this.minBoxSize,
                preferredAspectRatio = 1.0f,
                priority = Priority.MEDIUM
            )
        }
    }
    
    data class ObjectSettings(
        val minConfidence: Float,
        val minSize: Int,
        val preferredAspectRatio: Float,
        val priority: Priority
    )
    
    enum class Priority {
        LOW, MEDIUM, HIGH
    }
    
    // ═══════════════════════════════════════════════════════════
    // ADAPTIVE OPTIMIZATION
    // ═══════════════════════════════════════════════════════════
    
    fun autoOptimize(stats: PerformanceStats) {
        // Adjust based on FPS
        when {
            stats.averageFps >= 28 && stats.averageDetectionTimeMs < targetDetectionTimeMs -> {
                // Performance is good, can increase quality if needed
                if (currentMode == DetectionMode.PERFORMANCE) {
                    // Could upgrade to BALANCED
                }
            }
            
            stats.averageFps < 20 || stats.averageDetectionTimeMs > maxDetectionTimeMs -> {
                // Performance is struggling, reduce load
                if (currentMode == DetectionMode.QUALITY) {
                    setMode(DetectionMode.BALANCED)
                } else if (currentMode == DetectionMode.BALANCED) {
                    setMode(DetectionMode.PERFORMANCE)
                }
            }
        }
        
        // Adjust confidence based on detection success rate
        if (stats.successRate < 20) {
            // Too few detections, lower threshold
            minConfidence = maxOf(0.25f, minConfidence - 0.05f)
        } else if (stats.successRate > 80) {
            // Too many detections, raise threshold
            minConfidence = minOf(0.50f, minConfidence + 0.05f)
        }
    }
    
    data class PerformanceStats(
        val averageFps: Int,
        val averageDetectionTimeMs: Long,
        val successRate: Int,
        val totalDetections: Int
    )
    
    // ═══════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════
    
    fun reset() {
        setMode(DetectionMode.BALANCED)
    }
    
    fun getSummary(): String {
        return buildString {
            appendLine("╔═══════════════════════════════════════╗")
            appendLine("║     DETECTION CONFIGURATION          ║")
            appendLine("╠═══════════════════════════════════════╣")
            appendLine("║ Mode: $currentMode")
            appendLine("║ Confidence: $minConfidence")
            appendLine("║ IoU Threshold: $iouThreshold")
            appendLine("║ Min Box Size: $minBoxSize px")
            appendLine("║ Detection Interval: $baseDetectionInterval")
            appendLine("║ Tracking Timeout: ${trackingTimeoutMs}ms")
            appendLine("║ Smoothing: $smoothingFactor")
            appendLine("║ Stability Frames: $stabilityFrames")
            appendLine("║ GPU Acceleration: $enableGpuAcceleration")
            appendLine("╚═══════════════════════════════════════╝")
        }
    }
    
    fun printSettings() {
        android.util.Log.d("DetectionConfig", getSummary())
    }
    
    // ═══════════════════════════════════════════════════════════
    // PRESETS
    // ═══════════════════════════════════════════════════════════
    
    fun applyPreset(preset: Preset) {
        when (preset) {
            Preset.INDOOR -> {
                minConfidence = 0.35f
                minBoxSize = 30
                smoothingFactor = 0.35f
                trackingTimeoutMs = 2500L
            }
            
            Preset.OUTDOOR -> {
                minConfidence = 0.40f
                minBoxSize = 40
                smoothingFactor = 0.40f
                trackingTimeoutMs = 2000L
            }
            
            Preset.CLOSE_RANGE -> {
                minConfidence = 0.30f
                minBoxSize = 25
                smoothingFactor = 0.30f
                trackingTimeoutMs = 3000L
            }
            
            Preset.FAR_RANGE -> {
                minConfidence = 0.45f
                minBoxSize = 20
                smoothingFactor = 0.35f
                trackingTimeoutMs = 2000L
            }
        }
    }
    
    enum class Preset {
        INDOOR,
        OUTDOOR,
        CLOSE_RANGE,
        FAR_RANGE
    }
}