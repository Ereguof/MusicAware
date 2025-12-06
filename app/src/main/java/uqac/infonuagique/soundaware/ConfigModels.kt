package uqac.infonuagique.soundaware

import android.net.Uri

data class ContextConfig(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val requireHeadphones: Boolean,
    val requireLocation: Boolean,
    val locationLat: Double? = null,
    val locationLon: Double? = null,
    val locationRadius: Double? = null,
    val requireTime: Boolean = false,
    val timeStart: String? = null, // format "HH:mm"
    val timeEnd: String? = null,   // format "HH:mm"
    val requireActivity: Boolean = false,
    val requiredActivityType: Int? = null, // DetectedActivity.IN_VEHICLE, ON_FOOT, RUNNING, WALKING, etc.
    val minConfidence: Int = 75,           // Seuil de confiance (0-100)
    val playlist: List<Uri>
)
