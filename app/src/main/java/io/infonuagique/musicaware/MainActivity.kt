package io.infonuagique.musicaware

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.fence.AwarenessFence
import com.google.android.gms.awareness.fence.DetectedActivityFence
import com.google.android.gms.awareness.fence.HeadphoneFence
import com.google.android.gms.awareness.fence.LocationFence
import com.google.android.gms.awareness.fence.TimeFence
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.awareness.state.HeadphoneState
import com.google.android.gms.awareness.fence.FenceQueryRequest
import com.google.android.gms.awareness.fence.FenceState
import com.google.android.gms.awareness.fence.FenceUpdateRequest

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var fencePendingIntent: PendingIntent
    val STATUS_ACTION = "io.infonuagique.musicaware.AWARENESS_FENCE_STATUS"

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusTextView = TextView(this)
        setContentView(statusTextView)

        checkPermissions()

        val intent = Intent("io.infonuagique.musicaware.AWARENESS_FENCE")
        fencePendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        registerReceiver(fenceReceiver, IntentFilter(STATUS_ACTION), RECEIVER_EXPORTED)

        registerFences()
        queryFencesState()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACTIVITY_RECOGNITION
        )
        val missing = permissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 123)
        }
    }

    private val fenceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("FENCE_STATUS") ?: "Aucun signal"
            runOnUiThread { statusTextView.text = status }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun registerFences() {
        // Fence activité (ex: MARCHE)
        val walkingFence = DetectedActivityFence.during(DetectedActivity.WALKING)
        // Fence casque branché
        val headphonesFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN)
        // Fence temps (ex: entre 8h et 18h)
        val timeFence = TimeFence.inDailyInterval(null, 8L * 60L * 60L * 1000L, 18L * 60L * 60L * 1000L)
        // Fence localisation (ex: latitude/longitude fictives)
        val locationFence = LocationFence.`in`(37.4219983, -122.084, 100.0, 5L * 60L * 1000L) // Rayon de 100 mètres

        // Exemple de combinaison (marche ET casque branché)
        val combinedFence = AwarenessFence.and(walkingFence, headphonesFence)

        // Ajoutez ici d'autres combinaisons si besoin

        // Enregistrement des fences
        Awareness.getFenceClient(this).updateFences(
            FenceUpdateRequest.Builder()
                .addFence("WALKING", walkingFence, fencePendingIntent)
                .addFence("HEADPHONES", headphonesFence, fencePendingIntent)
//                .addFence("TIME", timeFence, fencePendingIntent)
//                .addFence("LOCATION", locationFence, fencePendingIntent)
//                .addFence("COMBINED", combinedFence, fencePendingIntent)
                .build()
        ).addOnSuccessListener {
            Log.d("FENCE", "Fences enregistrés")
        }.addOnFailureListener {
            Log.e("FENCE", "Erreur d’enregistrement des fences", it)
        }
    }

    private fun queryFencesState() {
        val fenceKeys = listOf("HEADPHONES", "WALKING"/*, "TIME", "LOCATION", "COMBINED"*/)
        Awareness.getFenceClient(this)
            .queryFences(FenceQueryRequest.forFences(fenceKeys))
            .addOnSuccessListener { response ->
                val states = fenceKeys.map { key ->
                    val state = response.fenceStateMap.getFenceState(key)
                    val status = when (state?.currentState) {
                        FenceState.TRUE -> "ACTIF"
                        FenceState.FALSE -> "INACTIF"
                        FenceState.UNKNOWN -> "INCONNU"
                        else -> "ÉTAT INCONNU"
                    }
                    "$key : $status"
                }
                runOnUiThread { statusTextView.text = states.joinToString("\n") }
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(fenceReceiver)
    }
}
