package uqac.infonuagique.soundaware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserContext(
    var headphonesConnected: Boolean = false,
    var location: String = "inconnue",
    var time: String = "00:00",
    // Activity fields
    var currentActivityType: Int = -1, // -1 or DetectedActivity.UNKNOWN
    var currentActivityConfidence: Int = 0
) {

    fun fetch(context: Context): UserContext {
        // 1. HEADPHONES CHECK
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        this.headphonesConnected = devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }

        // 2. TIME CHECK
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        this.time = sdf.format(Date())

        // 3. LOCATION CHECK
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (loc != null) {
                // Optional: Reverse Geocoding for city name, or just store lat/long string
                // For simplicity, we store coordinates or city if available
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    // Note: synchronous geocoder is discouraged on main thread but okay for simple prototype
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Async implementation omitted for brevity, using lat/long string fallback
                        this.location = "${loc.latitude},${loc.longitude}"
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        this.location = if (!addresses.isNullOrEmpty()) {
                            addresses[0].locality ?: "${loc.latitude},${loc.longitude}"
                        } else {
                            "${loc.latitude},${loc.longitude}"
                        }
                    }
                } catch (e: Exception) {
                    this.location = "${loc.latitude},${loc.longitude}"
                }
            } else {
                this.location = "Localisation non disponible"
            }
        } else {
            this.location = "permission manquante"
        }

        // 4. ACTIVITY RECOGNITION CHECK (Vital Fix)
        // We pull the latest values directly from the Receiver's static storage
        this.currentActivityType = ActivityRecognitionReceiver.getCurrentActivityTypeVal()
        this.currentActivityConfidence = ActivityRecognitionReceiver.getCurrentActivityConfidenceVal()

        return this
    }
}
