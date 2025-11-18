package uqac.infonuagique.soundaware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserContextData(
    val headphonesConnected: Boolean,
    val time: String,
    val location: String
)

class UserContext {
    fun fetch(context: Context): UserContextData {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var connected = false

        try {
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (d in devices) {
                when (d.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                        if (d.isSink) {
                            connected = true
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {}

        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date())

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val locationString = if (hasLocationPermission) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = lm.getProviders(true)
                var best: Location? = null
                for (p in providers) {
                    val l = lm.getLastKnownLocation(p)
                    if (l != null && (best == null || l.time > best.time)) {
                        best = l
                    }
                }
                if (best != null) {
                    String.format(Locale.getDefault(), "%.6f, %.6f", best.latitude, best.longitude)
                } else {
                    "inconnue"
                }
            } catch (e: Exception) {
                "erreur"
            }
        } else {
            "permission manquante"
        }

        return UserContextData(connected, time, locationString)
    }
}
