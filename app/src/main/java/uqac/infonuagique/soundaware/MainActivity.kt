package uqac.infonuagique.soundaware

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
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
import uqac.infonuagique.soundaware.ui.theme.SoundAwareTheme
import kotlin.collections.remove
import kotlin.rem
import kotlin.text.compareTo
import kotlin.text.get
import kotlin.toString

class MainActivity : ComponentActivity() {
    private val userContext = UserContext()
    private val LOCATION_PERMISSION_REQUEST = 1001

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
}

@Composable
fun ConfigsScreen(
    userContext: UserContext,
    requestLocationPermission: () -> Unit
) {
    val ctx = LocalContext.current
    val configs = ConfigRepository.getAll()
    var showEditor by remember { mutableStateOf<ContextConfig?>(null) }
    var playingConfigId by remember { mutableStateOf<Long?>(null) }
    var currentTrackIdx by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Affichage du snackbar si besoin
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // Gestion du MediaPlayer
    DisposableEffect(playingConfigId, currentTrackIdx, isPlaying) {
        if (playingConfigId != null && isPlaying) {
            isPaused = false
            val config = configs.find { it.id == playingConfigId }
            val playlist = config?.playlist ?: emptyList()
            val validPlaylist = playlist.filter { isUriAvailable(ctx, it) }
            if (validPlaylist.isNotEmpty() && currentTrackIdx < validPlaylist.size) {
                val uri = validPlaylist[currentTrackIdx]
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(ctx, uri)
                mediaPlayer?.setOnCompletionListener {
                    currentTrackIdx = (currentTrackIdx + 1) % validPlaylist.size
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
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            ctx, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPermission) requestLocationPermission()
                        val uc = userContext.fetch(ctx)
                        snackbarMessage = "Casque: ${if (uc.headphonesConnected) "Oui" else "Non"}, " +
                                "Position: ${uc.location}, " +
                                "Heure: ${uc.time}"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { Text("Obtenir contexte", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
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
                            playlist = emptyList()
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { Text("Nouvelle playlist", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                Button(
                    onClick = {
                        val uc = userContext.fetch(ctx)
                        val match = configs.firstOrNull { config ->
                            (!config.requireHeadphones || uc.headphonesConnected) &&
                                    (!config.requireLocation ||
                                            (uc.location != "inconnue" && uc.location != "permission manquante" &&
                                                    config.locationLat != null && config.locationLon != null && config.locationRadius != null &&
                                                    isInZone(uc.location, config.locationLat, config.locationLon, config.locationRadius))) &&
                                    (!config.requireTime ||
                                            (config.timeStart != null && config.timeEnd != null &&
                                                    isTimeInRange(uc.time.take(5), config.timeStart, config.timeEnd)))
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
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { Text("Lancer musique", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).padding(16.dp)) {
            LazyColumn {
                items(configs) { config ->
                    Card(Modifier.padding(4.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Text(config.name, style = MaterialTheme.typography.titleMedium)
                            Text("Casque requis: ${if (config.requireHeadphones) "Oui" else "Non"}")
                            Text("Localisation requise: ${if (config.requireLocation) "Oui" else "Non"}")
                            Text("Heure requise: ${if (config.requireTime) "Oui (${config.timeStart} - ${config.timeEnd})" else "Non"}")
                            if (config.requireLocation) {
                                Text("Zone: Lat=${config.locationLat}, Lon=${config.locationLon}, Rayon=${config.locationRadius}m")
                            }
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
                                        Button(onClick = {
                                            isPaused = true
                                            mediaPlayer?.pause()
                                        }) { Text("Pause") }
                                    } else {
                                        Button(onClick = {
                                            isPaused = false
                                            mediaPlayer?.start()
                                        }) { Text("Reprendre") }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = {
                                        val pl = config.playlist
                                        currentTrackIdx = (currentTrackIdx + 1) % pl.size
                                        isPaused = false
                                    }) { Text("Suivant") }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Lecture: ${currentTrackIdx + 1}/${config.playlist.size}")
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
                    if (ConfigRepository.getAll().any { it.id == config.id }) ConfigRepository.update(config)
                    else ConfigRepository.add(config)
                    ConfigRepository.save(ctx)
                    showEditor = null
                }
            )
        }
    }
}




// Utilitaire pour vérifier si la position actuelle est dans la zone
fun isInZone(locationStr: String, lat: Double, lon: Double, radius: Double): Boolean {
    val parts = locationStr.split(",")
    if (parts.size != 2) return false
    val userLat = parts[0].trim().toDoubleOrNull() ?: return false
    val userLon = parts[1].trim().toDoubleOrNull() ?: return false
    val results = FloatArray(1)
    android.location.Location.distanceBetween(userLat, userLon, lat, lon, results)
    return results[0] <= radius
}

fun isTimeInRange(current: String, start: String, end: String): Boolean {
    val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val now = fmt.parse(current)
    val s = fmt.parse(start)
    val e = fmt.parse(end)
    return if (s <= e) now in s..e else (now >= s || now <= e)
}

fun isUriAvailable(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.close()
        true
    } catch (e: Exception) {
        false
    }
}

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
    var playlist by remember { mutableStateOf(initial?.playlist ?: emptyList()) }
    var musicNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        playlist = playlist + uris
        uris.forEach { uri ->
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
        }
    }

    // Récupère les noms des fichiers
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
                    .heightIn(max = 400.dp) // Limite la hauteur de la boîte de dialogue
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
                    OutlinedTextField(
                        value = locationLat,
                        onValueChange = { locationLat = it },
                        label = { Text("Latitude") }
                    )
                    OutlinedTextField(
                        value = locationLon,
                        onValueChange = { locationLon = it },
                        label = { Text("Longitude") }
                    )
                    OutlinedTextField(
                        value = locationRadius,
                        onValueChange = { locationRadius = it },
                        label = { Text("Rayon (mètres)") }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requireTime, onCheckedChange = { requireTime = it })
                    Text("Heure requise")
                }
                if (requireTime) {
                    OutlinedTextField(
                        value = timeStart,
                        onValueChange = { timeStart = it },
                        label = { Text("Heure début (HH:mm)") }
                    )
                    OutlinedTextField(
                        value = timeEnd,
                        onValueChange = { timeEnd = it },
                        label = { Text("Heure fin (HH:mm)") }
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
                                timeStart = timeStart,
                                timeEnd = timeEnd,
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



