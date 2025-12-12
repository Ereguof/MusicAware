package uqac.infonuagique.soundaware

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse
import com.google.android.gms.awareness.snapshot.HeadphoneStateResponse
import com.google.android.gms.awareness.snapshot.LocationResponse
import com.google.android.gms.awareness.state.HeadphoneState
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class contenant les informations contextuelles actuelles de l'utilisateur,
 * collectées via l'API Awareness de Google Play Services.
 *
 * @property headphonesConnected Indique si un casque (filaire ou sans fil) est connecté.
 * @property time Heure actuelle au format "HH:mm".
 * @property location Position géographique au format "latitude, longitude" ou un message d'état
 *                    ("inconnue" ou "permission manquante").
 * @property currentActivityType Type d'activité physique détectée (constantes de [DetectedActivity]).
 * @property currentActivityConfidence Niveau de confiance de la détection d'activité (0-100).
 */
data class UserContextData(
    val headphonesConnected: Boolean = false,
    val time: String = "",
    val location: String = "inconnue",
    val currentActivityType: Int = DetectedActivity.STILL,
    val currentActivityConfidence: Int = 0
)

/**
 * Classe responsable de la récupération du contexte utilisateur en temps réel
 * à l'aide de l'API Google Awareness (Snapshot API).
 *
 * Les informations récupérées incluent :
 * - L'état du casque audio
 * - L'heure actuelle
 * - La position géographique
 * - L'activité physique détectée et son niveau de confiance
 *
 * Toutes les opérations sont effectuées de manière suspendue sur un thread IO afin de ne pas bloquer
 * le thread principal. Les erreurs sont capturées et loguées sans interrompre l'exécution.
 *
 * **Permissions requises** :
 * - [android.Manifest.permission.ACCESS_FINE_LOCATION] pour la localisation et l'activité.
 * L'annotation @SuppressLint("MissingPermission") est utilisée car la vérification des permissions
 * est supposée effectuée en amont par l'appelant.
 */
class UserContext {

    /**
     * Récupère de manière asynchrone les données contextuelles actuelles de l'utilisateur.
     *
     * Cette fonction doit être appelée depuis une coroutine (ex. : viewModelScope.launch ou
     * rememberCoroutineScope dans Compose).
     *
     * @param context Contexte de l'application, requis pour accéder au client Awareness.
     * @return Une instance de [UserContextData] contenant les valeurs détectées. Les valeurs par défaut
     *         sont utilisées en cas d'erreur sur un snapshot spécifique.
     */
    @SuppressLint("MissingPermission")
    suspend fun fetch(context: Context): UserContextData = withContext(Dispatchers.IO) {

        val snapshotClient = Awareness.getSnapshotClient(context)

        // 1. Heure actuelle (format HH:mm)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // 2. État du casque
        var headphonesConnected = false
        try {
            val headphoneResult: HeadphoneStateResponse = Tasks.await(snapshotClient.headphoneState)
            headphonesConnected = headphoneResult.headphoneState.state == HeadphoneState.PLUGGED_IN
        } catch (e: Exception) {
            Log.e("UserContext", "Error fetching Headphone State: ${e.message}")
        }

        // 3. Activité physique détectée et confiance associée
        var activityType = DetectedActivity.STILL
        var activityConfidence = 0
        try {
            val activityResult: DetectedActivityResponse = Tasks.await(snapshotClient.detectedActivity)
            val mostProbable = activityResult.activityRecognitionResult.mostProbableActivity
            activityType = mostProbable.type
            activityConfidence = mostProbable.confidence
        } catch (e: Exception) {
            Log.e("UserContext", "Error fetching Detected Activity: ${e.message}")
        }

        // 4. Position géographique
        var locationString = "inconnue"
        try {
            val locationResult: LocationResponse = Tasks.await(snapshotClient.location)
            val loc = locationResult.location
            locationString = String.format(Locale.US, "%.6f, %.6f", loc.latitude, loc.longitude)
        } catch (e: Exception) {
            Log.e("UserContext", "Error fetching Location: ${e.message}")
            // En cas de manque de permission, on pourrait affiner le message, mais l'appelant gère cela
        }

        // Retour des données collectées
        UserContextData(
            headphonesConnected = headphonesConnected,
            time = time,
            location = locationString,
            currentActivityType = activityType,
            currentActivityConfidence = activityConfidence
        )
    }
}