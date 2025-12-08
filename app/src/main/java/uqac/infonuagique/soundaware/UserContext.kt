package uqac.infonuagique.soundaware

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.snapshot.DetectedActivityResult
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult
import com.google.android.gms.awareness.snapshot.LocationResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.Tasks
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserContextData(
    val headphonesConnected: Boolean = false,
    val time: String = "",
    val location: String = "inconnue",
    val currentActivityType: Int = DetectedActivity.STILL,
    val currentActivityConfidence: Int = 0
)

class UserContext {

    @SuppressLint("MissingPermission") // On vérifie les permissions avant d'appeler fetch()
    fun fetch(context: Context): UserContextData {

        val snapshotClient = Awareness.getSnapshotClient(context)

        // 1. Heure actuelle
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // 2. Casque
        val headphoneTask = snapshotClient.headphoneState
        val headphoneResult: HeadphoneStateResult = Tasks.await(headphoneTask) as HeadphoneStateResult
        val headphonesConnected = if (headphoneResult.status.isSuccess) {
            headphoneResult.headphoneState?.state == 1 // 1 = PLUGGED_IN
        } else false

        // 3. Activité + confiance
        var activityType = DetectedActivity.STILL
        var activityConfidence = 0
        val activityTask = snapshotClient.detectedActivity
        val activityResult: DetectedActivityResult = Tasks.await(activityTask) as DetectedActivityResult
        if (activityResult.status.isSuccess) {
            val mostProbable = activityResult.activityRecognitionResult?.mostProbableActivity
            activityType = mostProbable?.type ?: 4
            activityConfidence = mostProbable?.confidence ?: 0
        }

        // 4. Position GPS
        var locationString = "inconnue"
        val locationTask = snapshotClient.location
        val locationResult: LocationResult = Tasks.await(locationTask) as LocationResult
        if (locationResult.status.isSuccess) {
            val loc = locationResult.location
            locationString = String.format(Locale.US, "%.6f, %.6f", loc?.latitude, loc?.longitude)
        }

        return UserContextData(
            headphonesConnected = headphonesConnected,
            time = time,
            location = locationString,
            currentActivityType = activityType,
            currentActivityConfidence = activityConfidence
        )
    }
}