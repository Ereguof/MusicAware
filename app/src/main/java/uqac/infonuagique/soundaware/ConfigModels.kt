package uqac.infonuagique.soundaware

import android.net.Uri

/**
 * Data class représentant une configuration de contexte pour l'application SoundAware.
 *
 * Cette classe définit les conditions qui déterminent quand un contexte spécifique doit être
 * considéré comme actif. Lorsque toutes les conditions requises sont satisfaites simultanément,
 * la playlist associée peut être automatiquement lancée ou proposée à l'utilisateur.
 *
 * Les conditions supportées incluent :
 * - Le port d'écouteurs (casque filaire ou sans fil)
 * - La proximité d'une zone géographique définie
 * - Une plage horaire spécifique
 * - Un type d'activité physique détectée avec un niveau de confiance minimal
 *
 * L'identifiant unique est généré automatiquement si aucune valeur n'est fournie lors de la création.
 */
data class ContextConfig(
    /**
     * Identifiant unique du contexte.
     * Par défaut, utilise le timestamp actuel en millisecondes pour assurer l'unicité.
     */
    val id: Long = System.currentTimeMillis(),

    /**
     * Nom descriptif du contexte, affiché à l'utilisateur (ex. : "En voiture", "Jogging matinal").
     */
    val name: String,

    /**
     * Indique si le port d'écouteurs est requis pour activer ce contexte.
     */
    val requireHeadphones: Boolean,

    /**
     * Indique si la proximité d'une zone géographique est requise pour activer ce contexte.
     */
    val requireLocation: Boolean,

    /**
     * Latitude du centre de la zone géographique (en degrés décimaux).
     * Valeur nulle si [requireLocation] est false.
     */
    val locationLat: Double? = null,

    /**
     * Longitude du centre de la zone géographique (en degrés décimaux).
     * Valeur nulle si [requireLocation] est false.
     */
    val locationLon: Double? = null,

    /**
     * Rayon de la zone géographique en mètres.
     * Valeur nulle si [requireLocation] est false.
     */
    val locationRadius: Double? = null,

    /**
     * Indique si une plage horaire spécifique est requise pour activer ce contexte.
     */
    val requireTime: Boolean = false,

    /**
     * Heure de début de la plage horaire au format "HH:mm".
     * Valeur nulle si [requireTime] est false.
     */
    val timeStart: String? = null,

    /**
     * Heure de fin de la plage horaire au format "HH:mm".
     * Valeur nulle si [requireTime] est false.
     */
    val timeEnd: String? = null,

    /**
     * Indique si un type d'activité physique spécifique est requis pour activer ce contexte.
     */
    val requireActivity: Boolean = false,

    /**
     * Type d'activité requis (constantes de [com.google.android.gms.location.DetectedActivity],
     * ex. : IN_VEHICLE, ON_FOOT, RUNNING, WALKING, STILL).
     * Valeur nulle si [requireActivity] est false.
     */
    val requiredActivityType: Int? = null,

    /**
     * Niveau de confiance minimal (en pourcentage, 0-100) requis pour valider la détection d'activité.
     * Valeur par défaut : 75 (seuil recommandé pour limiter les faux positifs).
     */
    val minConfidence: Int = 75,

    /**
     * Liste des URI des pistes audio composant la playlist associée à ce contexte.
     * Peut être vide lors de la création initiale.
     */
    val playlist: List<Uri>
)