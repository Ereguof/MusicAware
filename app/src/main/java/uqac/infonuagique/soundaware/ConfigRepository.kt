package uqac.infonuagique.soundaware

import androidx.compose.runtime.mutableStateListOf
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import androidx.core.net.toUri

object ConfigRepository {
    private val configs = mutableStateListOf<ContextConfig>()
    private const val PREFS_KEY = "configs"
    private val gson = Gson()

    fun getAll() = configs
    fun add(config: ContextConfig) {
        configs.add(config)
    }
    fun update(config: ContextConfig) {
        val idx = configs.indexOfFirst { it.id == config.id }
        if (idx != -1) configs[idx] = config
    }
    fun remove(id: Long) = configs.removeAll { it.id == id }

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
                playlist = it.playlist.map { uri -> uri.toString() }
            )
        }
        val json = gson.toJson(dtoList)
        prefs.edit { putString(PREFS_KEY, json) }
    }

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
                    // Vérifie que le fichier est accessible
                    context.contentResolver.openFileDescriptor(uri, "r")?.close()
                    uri
                } catch (_: Exception) {
                    null
                }
            }
            // Ignore les configs sans playlist valide
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
                playlist = uris
            )
        })
    }
}

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
    val playlist: List<String>
)


