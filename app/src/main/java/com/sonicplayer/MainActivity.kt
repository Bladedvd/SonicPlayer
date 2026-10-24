package com.sonicplayer

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var tracks by mutableStateOf<List<MusicTrack>>(emptyList())
    private var permissionGranted by mutableStateOf(false)
    private var currentTitle by mutableStateOf("Escolha uma música")
    private var currentArtist by mutableStateOf("Sua biblioteca local")
    private var isPlaying by mutableStateOf(false)
    private var search by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        permissionGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

        setContent {
            SonicTheme {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    permissionGranted = granted
                    if (granted) loadLibrary()
                }
                LaunchedEffect(Unit) {
                    if (permissionGranted) loadLibrary()
                    else launcher.launch(permission)
                }
                SonicHome(
                    tracks = tracks,
                    search = search,
                    onSearch = { search = it },
                    permissionGranted = permissionGranted,
                    currentTitle = currentTitle,
                    currentArtist = currentArtist,
                    isPlaying = isPlaying,
                    onPlayTrack = { index -> playTrack(index) },
                    onToggle = { togglePlayback() },
                    onNext = { mediaController?.seekToNextMediaItem() },
                    onRequestPermission = { launcher.launch(permission) }
                )
            }
        }

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                    override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                        currentTitle = metadata.title?.toString() ?: "Faixa sem título"
                        currentArtist = metadata.artist?.toString() ?: "Artista desconhecido"
                    }
                })
            } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadLibrary() {
        lifecycleScope.launch {
            tracks = withContext(Dispatchers.IO) { MusicRepository(this@MainActivity).loadTracks() }
        }
    }

    private fun playTrack(index: Int) {
        val list = tracks
        val controller = mediaController ?: return
        val items = list.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                ).build()
        }
        controller.setMediaItems(items, index, 0L)
        controller.prepare()
        controller.play()
    }

    private fun togglePlayback() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        super.onDestroy()
    }
}

@Composable
private fun SonicTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Color(0xFFB58CFF),
        secondary = Color(0xFF7C4DFF),
        background = Color(0xFF0B0B10),
        surface = Color(0xFF171720),
        onBackground = Color(0xFFF5F2FA),
        onSurface = Color(0xFFF5F2FA)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SonicHome(
    tracks: List<MusicTrack>,
    search: String,
    onSearch: (String) -> Unit,
    permissionGranted: Boolean,
    currentTitle: String,
    currentArtist: String,
    isPlaying: Boolean,
    onPlayTrack: (Int) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onRequestPermission: () -> Unit
) {
    var tab by remember { mutableStateOf("Músicas") }
    val filtered = remember(tracks, search) {
        tracks.withIndex().filter {
            val q = search.trim().lowercase()
            q.isEmpty() || it.value.title.lowercase().contains(q) ||
                it.value.artist.lowercase().contains(q) || it.value.album.lowercase().contains(q)
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth().clickable { tab = "Tocando" }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(44.dp).background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFF6D36D9), Color(0xFF1C8CFF))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.MusicNote, null, tint = Color.White) }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(currentTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(currentArtist, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f), fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = onToggle) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproduzir ou pausar")
                        }
                        IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Próxima") }
                    }
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf(
                        "Início" to Icons.Default.Home,
                        "Músicas" to Icons.Default.LibraryMusic,
                        "Álbuns" to Icons.Default.Album,
                        "Pastas" to Icons.Default.Folder,
                        "Ajustes" to Icons.Default.Settings
                    ).forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = tab == label,
                            onClick = { tab = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(18.dp))
            Text("SONIC PLAYER", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(
                when (tab) {
                    "Início" -> "Sua música, do seu jeito"
                    "Tocando" -> "Reproduzindo agora"
                    else -> tab
                },
                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp, bottom = 16.dp)
            )
            if (tab == "Ajustes") {
                Text("Sonic Player • versão 0.1.0", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
                Text("Biblioteca local • Reprodução Media3", modifier = Modifier.padding(top = 8.dp))
                Text("Mais temas, playlists e equalizador serão adicionados nas próximas etapas.", modifier = Modifier.padding(top = 8.dp))
            } else {
                OutlinedTextField(
                    value = search, onValueChange = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar música, artista ou álbum") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) IconButton(onClick = { onSearch("") }) {
                            Icon(Icons.Default.Close, "Limpar busca")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                if (!permissionGranted) {
                    Button(onClick = onRequestPermission) { Text("Permitir acesso às músicas") }
                } else if (tracks.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Nenhuma música encontrada", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                            Text("Adicione arquivos de áudio ao dispositivo.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f))
                        }
                    }
                } else {
                    Text("${filtered.size} músicas", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f), fontSize = 13.sp)
                    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(filtered, key = { _, pair -> pair.value.id }) { _, pair ->
                            val track = pair.value
                            Row(
                                Modifier.fillMaxWidth().clickable { onPlayTrack(pair.index) }.padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary) }
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(track.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${track.artist} • ${track.album}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .62f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { onPlayTrack(pair.index) }) {
                                    Icon(Icons.Default.PlayCircle, "Reproduzir ${track.title}", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
