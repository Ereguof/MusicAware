package uqac.infonuagique.soundaware

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient

// ... imports

object ActivityDetectionService {
    // Mettre 0 pour avoir les mises à jour le plus vite possible pendant les tests
    private const val DETECTION_INTERVAL_IN_MILLISECONDS: Long = 1000

    @SuppressLint("MissingPermission")
    fun startActivityRecognitionIfPermissionGranted(context: Context): Boolean {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val client: ActivityRecognitionClient = ActivityRecognition.getClient(context)
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            // MUTABLE est obligatoire pour Android 12+
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        println("SoundAware: Demande de mises à jour d'activité lancée...")

        client.removeActivityUpdates(pendingIntent)

        client.requestActivityUpdates(
            DETECTION_INTERVAL_IN_MILLISECONDS,
            pendingIntent
        )
            .addOnSuccessListener {
                println("SoundAware: Activity Updates SUCCESS")
            }
            .addOnFailureListener { e ->
                println("SoundAware: Activity Updates FAILED: ${e.message}")
            }

        return true
    }


    @SuppressLint("MissingPermission")
    fun stopActivityRecognition(context: Context) {
        val client = ActivityRecognition.getClient(context)
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        // Important: retirer les mises à jour pour économiser la batterie
        client.removeActivityUpdates(pendingIntent)
    }
}
