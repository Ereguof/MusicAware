package uqac.infonuagique.soundaware

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient

/**
 * Objet singleton responsable de la gestion de la détection continue d'activités physiques
 * de l'utilisateur à l'aide de l'API Activity Recognition de Google Play Services
 *
 * Ce service configure un [PendingIntent] vers [ActivityRecognitionReceiver] pour recevoir
 * des mises à jour périodiques sur l'activité la plus probable (marche, course, véhicule, etc.).
 *
 * L'intervalle de détection est fixé à 1 seconde pour une bonne réactivité, tout en restant
 * raisonnable en termes de consommation batterie.
 */
object ActivityDetectionService {

    /**
     * Intervalle entre deux demandes de mise à jour d'activité, en millisecondes.
     *
     * Une valeur de 1000 ms assure une détection relativement rapide des changements d'activité.
     * Augmenter cette valeur réduit la consommation de batterie au prix d'une latence accrue.
     */
    private const val DETECTION_INTERVAL_IN_MILLISECONDS: Long = 1000L

    /**
     * Démarre la reconnaissance d'activité si la permission requise est accordée.
     *
     * Cette méthode vérifie la permission [Manifest.permission.ACTIVITY_RECOGNITION],
     * supprime toute demande précédente pour éviter les doublons, puis enregistre
     * un nouveau [PendingIntent] auprès du client ActivityRecognition.
     *
     * Des messages de log sont affichés pour indiquer le succès ou l'échec de l'opération.
     *
     * @param context Contexte de l'application, nécessaire pour accéder au client et créer le PendingIntent.
     * @return `true` si la permission est accordée et la demande a été lancée ; `false` sinon.
     */
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
            101,  // Code de requête arbitraire, doit rester identique pour les opérations start/stop
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        println("SoundAware: Demande de mises à jour d'activité lancée...")

        // Suppression préalable des mises à jour existantes pour éviter les fuites ou doublons
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

    /**
     * Arrête la réception des mises à jour d'activité.
     *
     * Cette méthode reconstruit le même [PendingIntent] utilisé lors du démarrage
     * et demande au client de supprimer les mises à jour en cours.
     *
     * Il est recommandé d'appeler cette fonction dans [android.app.Activity.onDestroy]
     * ou lorsque la détection n'est plus nécessaire, afin d'économiser la batterie.
     *
     * @param context Contexte de l'application.
     */
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
        client.removeActivityUpdates(pendingIntent)
    }
}