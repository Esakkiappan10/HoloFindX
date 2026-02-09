package com.holofindx.ar

/**
 * 🏷️ Complete COCO Dataset Labels (80 classes)
 * Used by YOLOv8 for object detection
 */
object CocoLabels {
    val labels = listOf(
        "person",
        "bicycle",
        "car",
        "motorcycle",
        "airplane",
        "bus",
        "train",
        "truck",
        "boat",
        "traffic light",
        "fire hydrant",
        "stop sign",
        "parking meter",
        "bench",
        "bird",
        "cat",
        "dog",
        "horse",
        "sheep",
        "cow",
        "elephant",
        "bear",
        "zebra",
        "giraffe",
        "backpack",
        "umbrella",
        "handbag",
        "tie",
        "suitcase",
        "frisbee",
        "skis",
        "snowboard",
        "sports ball",
        "kite",
        "baseball bat",
        "baseball glove",
        "skateboard",
        "surfboard",
        "tennis racket",
        "bottle",
        "wine glass",
        "cup",
        "fork",
        "knife",
        "spoon",
        "bowl",
        "banana",
        "apple",
        "sandwich",
        "orange",
        "broccoli",
        "carrot",
        "hot dog",
        "pizza",
        "donut",
        "cake",
        "chair",
        "couch",
        "potted plant",
        "bed",
        "dining table",
        "toilet",
        "tv",
        "laptop",
        "mouse",
        "remote",
        "keyboard",
        "cell phone",
        "microwave",
        "oven",
        "toaster",
        "sink",
        "refrigerator",
        "book",
        "clock",
        "vase",
        "scissors",
        "teddy bear",
        "hair drier",
        "toothbrush"
    )
    
    /**
     * Get label by index (safe)
     */
    fun getLabel(index: Int): String? {
        return if (index in labels.indices) labels[index] else null
    }
    
    /**
     * Get index by label name
     */
    fun getIndex(label: String): Int {
        return labels.indexOf(label.lowercase())
    }
    
    /**
     * Check if label exists
     */
    fun hasLabel(label: String): Boolean {
        return labels.contains(label.lowercase())
    }
    
    /**
     * Get category type
     */
    fun getCategory(label: String): String {
        return when (label.lowercase()) {
            "person" -> "People"
            
            "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat" -> 
                "Vehicles"
            
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench" -> 
                "Outdoor"
            
            "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe" -> 
                "Animals"
            
            "backpack", "umbrella", "handbag", "tie", "suitcase" -> 
                "Accessories"
            
            "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat", 
            "baseball glove", "skateboard", "surfboard", "tennis racket" -> 
                "Sports"
            
            "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl" -> 
                "Kitchenware"
            
            "banana", "apple", "sandwich", "orange", "broccoli", "carrot", "hot dog", 
            "pizza", "donut", "cake" -> 
                "Food"
            
            "chair", "couch", "potted plant", "bed", "dining table", "toilet" -> 
                "Furniture"
            
            "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave", 
            "oven", "toaster", "sink", "refrigerator" -> 
                "Electronics & Appliances"
            
            "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush" -> 
                "Home Items"
            
            else -> "Other"
        }
    }
    
    /**
     * Get recommended color for label
     */
    fun getColor(label: String): Int {
        return when (label.lowercase()) {
            "person" -> android.graphics.Color.rgb(0, 255, 100)
            "car", "truck", "bus" -> android.graphics.Color.rgb(255, 50, 50)
            "cat", "dog" -> android.graphics.Color.rgb(255, 180, 0)
            "chair", "couch" -> android.graphics.Color.rgb(100, 150, 255)
            "laptop", "cell phone" -> android.graphics.Color.rgb(150, 0, 255)
            "cup", "bottle" -> android.graphics.Color.rgb(255, 255, 0)
            else -> android.graphics.Color.rgb(0, 255, 0)
        }
    }
}