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

/**
 * Data class représentant le contexte actuel de l'utilisateur dans l'application SoundAware.
 *
 * Cette classe regroupe les informations contextuelles détectées dynamiquement :
 * - Connexion d'écouteurs (filaires ou Bluetooth)
 * - Heure actuelle
 * - Position géographique (coordonnées ou nom de localité)
 * - Activité physique détectée et son niveau de confiance
 *
 * Les valeurs sont mises à jour via la méthode [fetch], qui interroge les services système
 * et le récepteur d'activité ([ActivityRecognitionReceiver]).
 */
data class UserContext(
    /**
     * Indique si un casque ou des écouteurs (filaires ou Bluetooth A2DP) sont connectés.
     */
    var headphonesConnected: Boolean = false,

    /**
     * Position géographique actuelle sous forme de chaîne :
     * - "latitude,longitude"
     * - Nom de la localité (si géocodage inverse réussi)
     * - "inconnue", "Localisation non disponible" ou "permission manquante" en cas d'erreur.
     */
    var location: String = "inconnue",

    /**
     * Heure actuelle au format "HH:mm".
     */
    var time: String = "00:00",

    /**
     * Type d'activité physique actuellement détectée (constantes de [com.google.android.gms.location.DetectedActivity]).
     * Valeur initiale : -1 (équivalent à UNKNOWN).
     */
    var currentActivityType: Int = -1,

    /**
     * Niveau de confiance de la détection d'activité (0-100).
     */
    var currentActivityConfidence: Int = 0
) {

    /**
     * Met à jour les propriétés de cette instance avec les valeurs contextuelles actuelles.
     *
     * Cette méthode interroge les services Android pour les écouteurs, l'heure et la localisation,
     * et récupère les données d'activité depuis [ActivityRecognitionReceiver].
     *
     * **Note** : Le géocodage inverse est effectué de manière synchrone pour simplifier le prototype.
     * Dans une application de production, il est recommandé d'utiliser une approche asynchrone
     * pour éviter tout blocage du thread principal.
     *
     * @param context Contexte de l'application, requis pour accéder aux services système.
     * @return L'instance courante mise à jour (pour chaînage fluide).
     */
    fun fetch(context: Context): UserContext {
        // 1. Détection des écouteurs connectés
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        this.headphonesConnected = devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }

        // 2. Heure actuelle
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        this.time = sdf.format(Date())

        // 3. Localisation géographique
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (loc != null) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Pour API 33+, une implémentation asynchrone serait préférable
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

        // 4. Activité physique (récupérée depuis le récepteur statique)
        this.currentActivityType = ActivityRecognitionReceiver.getCurrentActivityTypeVal()
        this.currentActivityConfidence = ActivityRecognitionReceiver.getCurrentActivityConfidenceVal()

        return this
    }
}