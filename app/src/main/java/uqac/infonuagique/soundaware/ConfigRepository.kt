package uqac.infonuagique.soundaware

import androidx.compose.runtime.mutableStateListOf
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import androidx.core.net.toUri

/**
 * Objet singleton servant de dépôt (repository) pour la gestion persistante des configurations
 * de contexte ([ContextConfig]) dans l'application SoundAware.
 *
 * Les configurations sont conservées en mémoire dans une liste observable ([mutableStateListOf])
 * afin de déclencher automatiquement les recompositions Jetpack Compose lors de modifications.
 * La persistance est assurée par [SharedPreferences] avec sérialisation JSON via Gson.
 *
 * Pour la sérialisation, les [android.net.Uri] de la playlist sont convertis en chaînes de caractères
 * au moyen d'un objet DTO ([ContextConfigDTO]). Lors du chargement, les permissions persistantes
 * sur ces URI sont restaurées et leur accessibilité est vérifiée. Les configurations dont
 * aucune piste audio n'est plus valide sont ignorées.
 */
object ConfigRepository {

    /**
     * Liste observable des configurations chargées ou créées.
     * Toute modification sur cette liste provoque une recomposition des Composables qui l'observent.
     */
    private val configs = mutableStateListOf<ContextConfig>()

    /** Clé utilisée pour stocker la liste sérialisée dans SharedPreferences. */
    private const val PREFS_KEY = "configs"

    /** Instance Gson dédiée à la sérialisation/désérialisation JSON. */
    private val gson = Gson()

    /**
     * Retourne une référence à la liste complète des configurations.
     *
     * @return Liste observable des [ContextConfig].
     */
    fun getAll() = configs

    /**
     * Ajoute une nouvelle configuration à la liste en mémoire.
     *
     * @param config Configuration à ajouter.
     */
    fun add(config: ContextConfig) {
        configs.add(config)
    }

    /**
     * Met à jour une configuration existante en remplaçant l'entrée correspondante
     * (recherche par identifiant unique).
     *
     * @param config Configuration mise à jour.
     */
    fun update(config: ContextConfig) {
        val idx = configs.indexOfFirst { it.id == config.id }
        if (idx != -1) configs[idx] = config
    }

    /**
     * Supprime toutes les configurations portant l'identifiant spécifié.
     *
     * @param id Identifiant de la configuration à supprimer.
     * @return true si au moins une configuration a été supprimée.
     */
    fun remove(id: Long) = configs.removeAll { it.id == id }

    /**
     * Sauvegarde l'ensemble des configurations actuelles dans SharedPreferences.
     *
     * Les objets [ContextConfig] sont convertis en [ContextConfigDTO] pour permettre
     * la sérialisation des URI sous forme de chaînes.
     *
     * @param context Contexte de l'application, requis pour accéder aux SharedPreferences.
     */
    fun save(context: Context) {
        val prefs = context.getSharedPreferences("soundaware", Context.MODE_PRIVATE)
        val dtoList = configs.map {
            ContextConfigDTO(
                id = it.id,
                name = it.name,
                requireHeadphones = it.requireHeadphones,
                requireLocation = it.requireLocation,
                locationLat = it.locationLat,
                locationLon = it.locationLon,
                locationRadius = it.locationRadius,
                requireTime = it.requireTime,
                timeStart = it.timeStart,
                timeEnd = it.timeEnd,
                requireActivity = it.requireActivity,
                requiredActivityType = it.requiredActivityType,
                minConfidence = it.minConfidence,
                playlist = it.playlist.map { uri -> uri.toString() }
            )
        }
        val json = gson.toJson(dtoList)
        prefs.edit { putString(PREFS_KEY, json) }
    }

    /**
     * Charge les configurations précédemment sauvegardées depuis SharedPreferences.
     *
     * Restaure les permissions persistantes sur les URI de la playlist et filtre
     * les configurations dont aucune piste audio n'est plus accessible.
     *
     * @param context Contexte de l'application, requis pour accéder aux SharedPreferences
     *                et au ContentResolver.
     */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences("soundaware", Context.MODE_PRIVATE)
        val json = prefs.getString(PREFS_KEY, null) ?: return
        val type = object : TypeToken<List<ContextConfigDTO>>() {}.type
        val list: List<ContextConfigDTO> = gson.fromJson(json, type)
        configs.clear()
        configs.addAll(list.mapNotNull {
            val uris = it.playlist.mapNotNull { s ->
                val uri = s.toUri()
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    context.contentResolver.openFileDescriptor(uri, "r")?.close()
                    uri
                } catch (_: Exception) {
                    null
                }
            }
            if (uris.isEmpty()) return@mapNotNull null
            ContextConfig(
                id = it.id,
                name = it.name,
                requireHeadphones = it.requireHeadphones,
                requireLocation = it.requireLocation,
                locationLat = it.locationLat,
                locationLon = it.locationLon,
                locationRadius = it.locationRadius,
                requireTime = it.requireTime,
                timeStart = it.timeStart,
                timeEnd = it.timeEnd,
                requireActivity = it.requireActivity,
                requiredActivityType = it.requiredActivityType,
                minConfidence = it.minConfidence,
                playlist = uris
            )
        })
    }
}

/**
 * Data class utilisée uniquement pour la sérialisation JSON des configurations de contexte.
 *
 * Les [android.net.Uri] ne pouvant être sérialisés directement par Gson, ils sont représentés
 * sous forme de chaînes de caractères ([String]) dans cette structure intermédiaire.
 */
data class ContextConfigDTO(
    val id: Long,
    val name: String,
    val requireHeadphones: Boolean,
    val requireLocation: Boolean,
    val locationLat: Double?,
    val locationLon: Double?,
    val locationRadius: Double?,
    val requireTime: Boolean,
    val timeStart: String?,
    val timeEnd: String?,
    val requireActivity: Boolean,
    val requiredActivityType: Int?,
    val minConfidence: Int,
    val playlist: List<String>
)