package uqac.infonuagique.soundaware

import android.net.Uri

/**
 * Data class représentant une configuration de contexte pour l'application SoundAware.
 *
 * Cette classe définit les règles qui déterminent quand un contexte spécifique doit être considéré
 * comme actif. Lorsqu'un contexte est actif, la playlist associée peut être automatiquement sélectionnée
 * ou proposée à l'utilisateur.
 *
 * Un contexte est validé si toutes les conditions activées (requireX = true) sont satisfaites simultanément.
 * Les conditions incluent l'utilisation d'écouteurs, la localisation géographique, une plage horaire
 * et/ou un type d'activité physique détectée.
 *
 * L'identifiant unique est généré automatiquement à la création si aucune valeur n'est fournie.
 */
data class ContextConfig(
    /**
     * Identifiant unique du contexte.
     * Par défaut, utilise le timestamp actuel en millisecondes pour garantir l'unicité.
     */
    val id: Long = System.currentTimeMillis(),

    /**
     * Nom descriptif du contexte, visible par l'utilisateur (ex. : "Au bureau", "En jogging", "En voiture").
     */
    val name: String,

    /**
     * Indique si le port d'écouteurs (casque ou écouteurs filaires/sans fil) est requis pour activer ce contexte.
     */
    val requireHeadphones: Boolean,

    /**
     * Indique si une position géographique spécifique est requise pour activer ce contexte.
     */
    val requireLocation: Boolean,

    /**
     * Latitude du centre de la zone géographique (en degrés décimaux).
     * Valeur nulle si [requireLocation] est false ou non définie.
     */
    val locationLat: Double? = null,

    /**
     * Longitude du centre de la zone géographique (en degrés décimaux).
     * Valeur nulle si [requireLocation] est false ou non définie.
     */
    val locationLon: Double? = null,

    /**
     * Rayon de la zone géographique en mètres.
     * Valeur nulle si [requireLocation] est false ou non définie.
     */
    val locationRadius: Double? = null,

    /**
     * Indique si une plage horaire spécifique est requise pour activer ce contexte.
     */
    val requireTime: Boolean = false,

    /**
     * Heure de début de la plage horaire (format attendu : "HH:mm").
     * Valeur nulle si [requireTime] est false.
     */
    val timeStart: String? = null,

    /**
     * Heure de fin de la plage horaire (format attendu : "HH:mm").
     * Valeur nulle si [requireTime] est false.
     */
    val timeEnd: String? = null,

    /**
     * Indique si un type d'activité physique spécifique est requis pour activer ce contexte.
     */
    val requireActivity: Boolean = false,

    /**
     * Type d'activité requis (correspond aux constantes de [DetectedActivity], ex. : [DetectedActivity.IN_VEHICLE]).
     * Valeur nulle si [requireActivity] est false.
     */
    val requiredActivityType: Int? = null,

    /**
     * Niveau de confiance minimal (en pourcentage, 0-100) requis pour valider la détection d'activité.
     * Valeur par défaut : 75 (seuil recommandé pour éviter les faux positifs).
     */
    val minConfidence: Int = 75,

    /**
     * Liste des URI pointant vers les pistes audio de la playlist associée à ce contexte.
     * Peut être vide si aucune playlist n'est encore définie.
     */
    val playlist: List<Uri>
)