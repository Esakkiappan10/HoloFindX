package com.holofindx.ar

import android.graphics.Rect

data class DetectedObjectBox(
    val rect: Rect,
    val label: String,
    val confidence: Float
)
