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
 * Les configurations sont stockées en mémoire dans une liste observable ([mutableStateListOf])
 * pour permettre une mise à jour automatique de l'interface Compose. La persistance est assurée
 * via [SharedPreferences] avec sérialisation JSON (Gson).
 *
 * Lors du chargement, les URI de la playlist sont reconvertis et validés : les permissions persistantes
 * sont reprises et l'accessibilité des fichiers est vérifiée. Les configurations dont tous les
 * fichiers audio sont devenus inaccessibles sont ignorées.
 *
 * Un objet DTO ([ContextConfigDTO]) est utilisé pour la sérialisation, car les [android.net.Uri]
 * ne sont pas directement sérialisables par Gson.
 */
object ConfigRepository {

    /**
     * Liste observable contenant toutes les configurations chargées ou créées.
     * Les modifications sur cette liste déclenchent automatiquement la recomposition des Composables.
     */
    private val configs = mutableStateListOf<ContextConfig>()

    /** Clé utilisée dans SharedPreferences pour stocker la liste sérialisée. */
    private const val PREFS_KEY = "configs"

    /** Instance Gson pour la sérialisation/désérialisation JSON. */
    private val gson = Gson()

    /**
     * Retourne une vue immuable de la liste complète des configurations.
     *
     * @return Liste des [ContextConfig] actuellement chargées.
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
     * Met à jour une configuration existante (recherche par [ContextConfig.id]).
     *
     * @param config Configuration mise à jour.
     */
    fun update(config: ContextConfig) {
        val idx = configs.indexOfFirst { it.id == config.id }
        if (idx != -1) configs[idx] = config
    }

    /**
     * Supprime toutes les configurations correspondant à l'identifiant fourni.
     *
     * @param id Identifiant de la configuration à supprimer.
     * @return true si au moins une configuration a été supprimée.
     */
    fun remove(id: Long) = configs.removeAll { it.id == id }

    /**
     * Sauvegarde toutes les configurations actuelles dans SharedPreferences.
     *
     * Convertit les [ContextConfig] en [ContextConfigDTO] pour sérialiser les URI sous forme de chaînes.
     *
     * @param context Contexte de l'application requis pour accéder aux SharedPreferences.
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
     * Charge les configurations sauvegardées depuis SharedPreferences.
     *
     * Restaure les permissions persistantes sur les URI et filtre les configurations
     * dont aucun fichier audio n'est plus accessible.
     *
     * @param context Contexte de l'application requis pour accéder aux SharedPreferences
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
                    // Vérification de l'accessibilité du fichier
                    context.contentResolver.openFileDescriptor(uri, "r")?.close()
                    uri
                } catch (_: Exception) {
                    null
                }
            }
            // Ignore les configurations sans aucune piste audio valide
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
 * Data class utilisée exclusivement pour la sérialisation JSON des configurations.
 *
 * Les [android.net.Uri] ne sont pas sérialisables directement par Gson ; ils sont donc
 * convertis en chaînes de caractères ([String]) pour le stockage.
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