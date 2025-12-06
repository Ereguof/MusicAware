package uqac.infonuagique.soundaware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

// CHANGE: Must be a 'class', not an 'object' so Android can create new instances of it
class ActivityRecognitionReceiver : BroadcastReceiver() {

    // We use a companion object to store the state globally,
    // so different instances of the receiver (created by Android) can update the same variables.
    companion object {
        // Variables must be volatile or synchronized if accessed from multiple threads,
        // but for simple UI updates, this is usually fine.
        var currentActivityType: Int = DetectedActivity.UNKNOWN
            private set // Only allow modification inside this class

        var currentConfidence: Int = 0
            private set

        // Static helper methods to access data from MainActivity
        fun getCurrentActivityTypeVal(): Int = currentActivityType
        fun getCurrentActivityConfidenceVal(): Int = currentConfidence

        fun getActivityName(type: Int): String = when (type) {
            DetectedActivity.IN_VEHICLE -> "En véhicule"
            DetectedActivity.ON_BICYCLE -> "À vélo"
            DetectedActivity.ON_FOOT    -> "À pied"
            DetectedActivity.RUNNING   -> "Course à pied"
            DetectedActivity.WALKING   -> "Marche"
            DetectedActivity.STILL     -> "Immobile"
            else                       -> "Inconnu ($type)"
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("test","Test")
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val mostProbableActivity = result?.mostProbableActivity
            if (mostProbableActivity != null) {
                // Update the static variables in the companion object
                currentActivityType = mostProbableActivity.type
                currentConfidence = mostProbableActivity.confidence
                println("Activity Update: ${getActivityName(currentActivityType)} ($currentConfidence%)")
            }
        }
    }
}
