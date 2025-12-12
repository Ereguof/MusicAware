package uqac.infonuagique.soundaware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * BroadcastReceiver dédié à la réception des mises à jour d'activité provenant de l'API
 * Activity Recognition de Google Play Services.
 *
 * Ce receiver est invoqué par le système lorsqu'une nouvelle détection d'activité est disponible
 * via le [PendingIntent] configuré dans [ActivityDetectionService]. Il extrait l'activité la plus
 * probable, met à jour les valeurs statiques partagées dans le companion object, et affiche un
 * message dans la console pour indiquer le type d'activité détecté et son niveau de confiance.
 *
 * La classe est implémentée comme une `class` (et non un `object`) afin que le système Android
 * puisse instancier de nouvelles instances si nécessaire. Les données détectées sont stockées
 * dans le companion object pour être accessibles de manière globale et thread-safe depuis
 * n'importe quelle partie de l'application.
 */
class ActivityRecognitionReceiver : BroadcastReceiver() {

    /**
     * Companion object contenant les données d'activité actuellement détectées ainsi que des
     * utilitaires pour y accéder et interpréter les types d'activité.
     *
     * Les propriétés sont privées avec des setters privés afin d'assurer que seules les mises à
     * jour provenant de ce receiver puissent les modifier.
     */
    companion object {

        /**
         * Type de l'activité actuellement détectée (une des constantes de [DetectedActivity]).
         * Valeur initiale : [DetectedActivity.UNKNOWN].
         */
        var currentActivityType: Int = DetectedActivity.UNKNOWN
            private set

        /**
         * Niveau de confiance (en pourcentage) de la détection actuelle.
         * Valeur initiale : 0.
         */
        var currentConfidence: Int = 0
            private set

        /**
         * Retourne le type d'activité actuel.
         *
         * @return Le code du type d'activité détecté.
         */
        fun getCurrentActivityTypeVal(): Int = currentActivityType

        /**
         * Retourne le niveau de confiance actuel de la détection.
         *
         * @return Le pourcentage de confiance (0-100).
         */
        fun getCurrentActivityConfidenceVal(): Int = currentConfidence

        /**
         * Convertit un code de type d'activité en une chaîne de caractères lisible en français.
         *
         * @param type Le code du type d'activité ([DetectedActivity]).
         * @return Une description textuelle de l'activité, ou "Inconnu" si le type n'est pas reconnu.
         */
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

    /**
     * Méthode appelée par le système lors de la réception d'une Intent correspondant à une mise
     * à jour d'activité.
     *
     * Vérifie la présence d'un [ActivityRecognitionResult] dans l'Intent, extrait l'activité la
     * plus probable, met à jour les valeurs statiques du companion object, et affiche un message
     * dans la console.
     *
     * @param context Le contexte de l'application.
     * @param intent L'Intent reçue contenant potentiellement les données d'activité.
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("test", "Test")
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val mostProbableActivity = result?.mostProbableActivity
            if (mostProbableActivity != null) {
                currentActivityType = mostProbableActivity.type
                currentConfidence = mostProbableActivity.confidence
                println("Activity Update: ${getActivityName(currentActivityType)} ($currentConfidence%)")
            }
        }
    }
}