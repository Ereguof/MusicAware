package uqac.infonuagique.soundaware

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.launch
import uqac.infonuagique.soundaware.ui.theme.SoundAwareTheme

/**
 * Activité principale de l'application SoundAware.
 *
 * Cette activité initialise les services nécessaires (chargement des configurations, détection d'activité),
 * gère les permissions requises et affiche l'écran principal composé avec Jetpack Compose.
 */
class MainActivity : ComponentActivity() {

    private val userContext = UserContext()
    private val LOCATION_PERMISSION_REQUEST = 1001

    /**
     * Méthode de cycle de vie : initialisation de l'activité.
     *
     * Charge les configurations sauvegardées, demande ou démarre la détection d'activité selon les permissions,
     * active le mode edge-to-edge et définit le contenu Compose.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfigRepository.load(this)

        enableEdgeToEdge()
        setContent {
            SoundAwareTheme {
                ConfigsScreen(
                    userContext = userContext,
                    requestLocationPermission = {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            LOCATION_PERMISSION_REQUEST
                        )
                    }
                )
            }
        }
    }

    /**
     * Méthode de cycle de vie : libération des ressources à la destruction de l'activité.
     */
    override fun onDestroy() {
        super.onDestroy()
    }
}

/**
 * Écran principal de l'application affichant la liste des configurations de contexte
 * et permettant leur gestion (création, modification, suppression, lecture).
 *
 * @param userContext Instance de [UserContext] utilisée pour récupérer le contexte actuel de l'utilisateur.
 * @param requestLocationPermission Callback pour demander la permission de localisation fine.
 */
@Composable
fun ConfigsScreen(
    userContext: UserContext,
    requestLocationPermission: () -> Unit
) {
    val ctx = LocalContext.current

    val scope = rememberCoroutineScope()

    val configs = ConfigRepository.getAll()
    var showEditor by remember { mutableStateOf<ContextConfig?>(null) }
    var playingConfigId by remember { mutableStateOf<Long?>(null) }
    var currentTrackIdx by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    DisposableEffect(playingConfigId, currentTrackIdx, isPlaying) {
        if (playingConfigId != null && isPlaying) {
            isPaused = false
            val config = configs.find { it.id == playingConfigId }
            val playlist = config?.playlist?.filter { isUriAvailable(ctx, it) } ?: emptyList()
            if (playlist.isNotEmpty() && currentTrackIdx < playlist.size) {
                val uri = playlist[currentTrackIdx]
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(ctx, uri)
                mediaPlayer?.setOnCompletionListener {
                    currentTrackIdx = (currentTrackIdx + 1) % playlist.size
                }
                mediaPlayer?.start()
            }
        }
        onDispose { mediaPlayer?.release() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            if (ContextCompat.checkSelfPermission(
                                    ctx, Manifest.permission.ACCESS_FINE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestLocationPermission()
                            }
                            val uc = userContext.fetch(ctx)
                            snackbarMessage = buildString {
                                append("Casque: ${if (uc.headphonesConnected) "Oui" else "Non"}\n")
                                append("Position: ${uc.location}\n")
                                append("Heure: ${uc.time}\n")
                                append("Activité: ${getActivityName(uc.currentActivityType)} ")
                                append("(${uc.currentActivityConfidence}%)")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Obtenir contexte", textAlign = TextAlign.Center)
                }

                Button(
                    onClick = {
                        showEditor = ContextConfig(
                            id = System.currentTimeMillis(),
                            name = "",
                            requireHeadphones = false,
                            requireLocation = false,
                            locationLat = null,
                            locationLon = null,
                            locationRadius = null,
                            requireTime = false,
                            timeStart = null,
                            timeEnd = null,
                            requireActivity = false,
                            requiredActivityType = null,
                            minConfidence = 75,
                            playlist = emptyList()
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Nouvelle playlist", textAlign = TextAlign.Center)
                }

                Button(
                    onClick = {
                        scope.launch {
                            val uc = userContext.fetch(ctx)
                            val match = configs.firstOrNull { config ->
                                (!config.requireHeadphones || uc.headphonesConnected) &&
                                        (!config.requireLocation || (
                                                uc.location != "inconnue" && uc.location != "permission manquante" &&
                                                        config.locationLat != null && config.locationLon != null && config.locationRadius != null &&
                                                        isInZone(uc.location, config.locationLat, config.locationLon, config.locationRadius)
                                                )) &&
                                        (!config.requireTime || (
                                                config.timeStart != null && config.timeEnd != null &&
                                                        isTimeInRange(uc.time.take(5), config.timeStart, config.timeEnd)
                                                )) &&
                                        (!config.requireActivity || (
                                                uc.currentActivityType == config.requiredActivityType &&
                                                        uc.currentActivityConfidence >= config.minConfidence
                                                ))
                            }

                            if (match != null && match.playlist.isNotEmpty()) {
                                playingConfigId = match.id
                                currentTrackIdx = 0
                                isPlaying = true
                            } else {
                                playingConfigId = null
                                isPlaying = false
                                snackbarMessage = "Aucune configuration ne correspond au contexte actuel."
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Lancer musique", textAlign = TextAlign.Center)
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            LazyColumn {
                items(configs) { config ->
                    Card(modifier = Modifier.padding(4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(config.name, style = MaterialTheme.typography.titleMedium)
                            Text("Casque: ${if (config.requireHeadphones) "Oui" else "Non"}")
                            Text("Localisation: ${if (config.requireLocation) "Oui" else "Non"}")
                            Text("Heure: ${if (config.requireTime) "Oui (${config.timeStart}–${config.timeEnd})" else "Non"}")
                            if (config.requireLocation) {
                                Text("Zone: ${config.locationLat}, ${config.locationLon} (±${config.locationRadius}m)")
                            }
                            Text("Activité requise: ${if (config.requireActivity) "Oui" else "Non"}")
                            Text("Musiques: ${config.playlist.size}")

                            Row {
                                Button(onClick = { showEditor = config }) { Text("Modifier") }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = {
                                    ConfigRepository.remove(config.id)
                                    ConfigRepository.save(ctx)
                                }) { Text("Supprimer") }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = {
                                    playingConfigId = config.id
                                    currentTrackIdx = 0
                                    isPlaying = true
                                }) { Text("Jouer") }
                            }

                            if (playingConfigId == config.id && isPlaying) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isPaused) {
                                        Button(onClick = { isPaused = true; mediaPlayer?.pause() }) { Text("Pause") }
                                    } else {
                                        Button(onClick = { isPaused = false; mediaPlayer?.start() }) { Text("Reprendre") }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = {
                                        currentTrackIdx = (currentTrackIdx + 1) % config.playlist.size
                                        isPaused = false
                                    }) { Text("Suivant") }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Piste ${currentTrackIdx + 1}/${config.playlist.size}")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEditor != null) {
            ConfigEditorDialog(
                initial = showEditor,
                onDismiss = { showEditor = null },
                onSave = { config ->
                    if (configs.any { it.id == config.id }) {
                        ConfigRepository.update(config)
                    } else {
                        ConfigRepository.add(config)
                    }
                    ConfigRepository.save(ctx)
                    showEditor = null
                }
            )
        }
    }
}

/**
 * Vérifie si l'utilisateur se trouve dans la zone géographique définie.
 *
 * @param locationStr Chaîne "latitude, longitude" de la position actuelle.
 * @param lat Latitude du centre de la zone.
 * @param lon Longitude du centre de la zone.
 * @param radius Rayon en mètres.
 * @return true si la distance ≤ rayon.
 */
fun isInZone(locationStr: String, lat: Double, lon: Double, radius: Double): Boolean {
    val parts = locationStr.split(",")
    if (parts.size != 2) return false
    val userLat = parts[0].trim().toDoubleOrNull() ?: return false
    val userLon = parts[1].trim().toDoubleOrNull() ?: return false
    val results = FloatArray(1)
    android.location.Location.distanceBetween(userLat, userLon, lat, lon, results)
    return results[0] <= radius
}

/**
 * Vérifie si l'heure actuelle est dans la plage définie (gère le passage par minuit).
 *
 * @param current Heure actuelle "HH:mm".
 * @param start Heure de début "HH:mm".
 * @param end Heure de fin "HH:mm".
 * @return true si dans la plage.
 */
fun isTimeInRange(current: String, start: String, end: String): Boolean {
    val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val now = fmt.parse(current)
    val s = fmt.parse(start)
    val e = fmt.parse(end)
    if (now != null && s != null && e != null) {
        return if (s <= e) now.time in s.time..e.time else (now.time >= s.time || now.time <= e.time)
    }
    return false
}

/**
 * Vérifie la disponibilité d'un URI audio (permission persistante valide).
 *
 * @param context Contexte de l'application.
 * @param uri URI à tester.
 * @return true si accessible.
 */
fun isUriAvailable(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.close()
        true
    } catch (e: Exception) {
        false
    }
}

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

/**
 * Dialogue d'édition/création d'une configuration de contexte.
 *
 * @param initial Configuration initiale (null pour création).
 * @param onDismiss Fermeture sans sauvegarde.
 * @param onSave Sauvegarde de la configuration modifiée.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigEditorDialog(
    initial: ContextConfig?,
    onDismiss: () -> Unit,
    onSave: (ContextConfig) -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var requireHeadphones by remember { mutableStateOf(initial?.requireHeadphones ?: false) }
    var requireLocation by remember { mutableStateOf(initial?.requireLocation ?: false) }
    var locationLat by remember { mutableStateOf(initial?.locationLat?.toString() ?: "") }
    var locationLon by remember { mutableStateOf(initial?.locationLon?.toString() ?: "") }
    var locationRadius by remember { mutableStateOf(initial?.locationRadius?.toString() ?: "") }
    var requireTime by remember { mutableStateOf(initial?.requireTime ?: false) }
    var timeStart by remember { mutableStateOf(initial?.timeStart ?: "") }
    var timeEnd by remember { mutableStateOf(initial?.timeEnd ?: "") }
    var requireActivity by remember { mutableStateOf(initial?.requireActivity ?: false) }
    var requiredActivityType by remember { mutableIntStateOf(initial?.requiredActivityType ?: DetectedActivity.UNKNOWN) }
    var minConfidenceStr by remember { mutableStateOf(initial?.minConfidence?.toString() ?: "75") }
    var playlist by remember { mutableStateOf(initial?.playlist ?: emptyList<Uri>()) }
    var musicNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        uris?.let {
            playlist = playlist + it
            it.forEach { uri ->
                try {
                    ctx.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(playlist) {
        musicNames = playlist.map { uri ->
            val cursor = ctx.contentResolver.query(uri, null, null, null, null)
            var name: String? = null
            cursor?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && idx != -1) {
                    name = c.getString(idx)
                }
            }
            name ?: uri.lastPathSegment ?: uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle configuration" else "Modifier la configuration") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requireHeadphones, onCheckedChange = { requireHeadphones = it })
                    Text("Casque requis")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requireLocation, onCheckedChange = { requireLocation = it })
                    Text("Localisation requise")
                }
                if (requireLocation) {
                    OutlinedTextField(value = locationLat, onValueChange = { locationLat = it }, label = { Text("Latitude") })
                    OutlinedTextField(value = locationLon, onValueChange = { locationLon = it }, label = { Text("Longitude") })
                    OutlinedTextField(value = locationRadius, onValueChange = { locationRadius = it }, label = { Text("Rayon (mètres)") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requireTime, onCheckedChange = { requireTime = it })
                    Text("Heure requise")
                }
                if (requireTime) {
                    OutlinedTextField(value = timeStart, onValueChange = { timeStart = it }, label = { Text("Heure début (HH:mm)") })
                    OutlinedTextField(value = timeEnd, onValueChange = { timeEnd = it }, label = { Text("Heure fin (HH:mm)") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requireActivity, onCheckedChange = { requireActivity = it })
                    Text("Activité physique requise")
                }
                if (requireActivity) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = when (requiredActivityType) {
                                DetectedActivity.IN_VEHICLE -> "En véhicule"
                                DetectedActivity.RUNNING -> "Course à pied"
                                DetectedActivity.WALKING -> "Marche"
                                DetectedActivity.ON_FOOT -> "À pied (général)"
                                DetectedActivity.STILL -> "Immobile"
                                else -> "Non défini"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type d'activité") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(
                                DetectedActivity.IN_VEHICLE to "En véhicule",
                                DetectedActivity.RUNNING to "Course à pied",
                                DetectedActivity.WALKING to "Marche",
                                DetectedActivity.ON_FOOT to "À pied (général)",
                                DetectedActivity.STILL to "Immobile"
                            ).forEach { (type, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    requiredActivityType = type
                                    expanded = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = minConfidenceStr,
                        onValueChange = { minConfidenceStr = it },
                        label = { Text("Confiance minimale (%)") }
                    )
                }

                Button(onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) }) { Text("Ajouter des musiques") }
                Text("Playlist :")
                musicNames.forEachIndexed { idx, n ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• $n", modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                playlist = playlist.toMutableList().also { it.removeAt(idx) }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Supprimer")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && playlist.isNotEmpty() &&
                        (!requireLocation || (locationLat.isNotBlank() && locationLon.isNotBlank() && locationRadius.isNotBlank()))
                    ) {
                        onSave(
                            ContextConfig(
                                id = initial?.id ?: System.currentTimeMillis(),
                                name = name,
                                requireHeadphones = requireHeadphones,
                                requireLocation = requireLocation,
                                locationLat = locationLat.toDoubleOrNull(),
                                locationLon = locationLon.toDoubleOrNull(),
                                locationRadius = locationRadius.toDoubleOrNull(),
                                requireTime = requireTime,
                                timeStart = timeStart.ifBlank { null },
                                timeEnd = timeEnd.ifBlank { null },
                                requireActivity = requireActivity,
                                requiredActivityType = if (requireActivity) requiredActivityType else null,
                                minConfidence = minConfidenceStr.toIntOrNull() ?: 75,
                                playlist = playlist
                            )
                        )
                    }
                }
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Annuler") }
        }
    )
}