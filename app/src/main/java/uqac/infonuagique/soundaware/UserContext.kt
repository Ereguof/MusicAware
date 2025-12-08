package uqac.infonuagique.soundaware

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse
import com.google.android.gms.awareness.snapshot.DetectedActivityResult
import com.google.android.gms.awareness.snapshot.HeadphoneStateResponse
import com.google.android.gms.awareness.snapshot.HeadphoneStateResult
import com.google.android.gms.awareness.snapshot.LocationResponse
import com.google.android.gms.awareness.snapshot.LocationResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    suspend fun fetch(context: Context): UserContextData = withContext(Dispatchers.IO) {

        val snapshotClient = Awareness.getSnapshotClient(context)

        // 1. Heure actuelle
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // 2. Casque
        val headphoneTask = snapshotClient.headphoneState
        // Tasks.await is safe here because we are on the IO dispatcher
        val headphoneResult: HeadphoneStateResponse = Tasks.await(headphoneTask)
        val headphonesConnected = headphoneResult.headphoneState.state == 1

        // 3. Activité + confiance
        var activityType = DetectedActivity.STILL
        var activityConfidence = 0
        val activityTask = snapshotClient.detectedActivity
        val activityResult: DetectedActivityResponse = Tasks.await(activityTask)

        val mostProbable = activityResult.activityRecognitionResult.mostProbableActivity
        activityType = mostProbable.type
        activityConfidence = mostProbable.confidence

        // 4. Position GPS
        var locationString = "inconnue"
        val locationTask = snapshotClient.location
        val locationResult: LocationResponse = Tasks.await(locationTask)
        val loc = locationResult.location
        locationString = String.format(Locale.US, "%.6f, %.6f", loc.latitude, loc.longitude)

        // Return the result from the IO block
        UserContextData(
            headphonesConnected = headphonesConnected,
            time = time,
            location = locationString,
            currentActivityType = activityType,
            currentActivityConfidence = activityConfidence
        )
    }
}
