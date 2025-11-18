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
    val playlist: List<Uri>
)
