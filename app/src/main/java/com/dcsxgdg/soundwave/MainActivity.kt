package com.dcsxgdg.soundwave

import android.Manifest
import androidx.core.net.toUri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.dcsxgdg.soundwave.data.Song

private val Ink = Color(0xFF0B0D12)
private val Panel = Color(0xFF151922)
private val Muted = Color(0xFF9AA3B2)
private val Mint = Color(0xFF8DF5C2)
private val Violet = Color(0xFF9A8CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val musicViewModel: MusicViewModel = viewModel()
            SoundwaveApp(musicViewModel)
        }
    }
}

@Composable
private fun SoundwaveApp(viewModel: MusicViewModel) {
    val state by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink,
            surface = Panel,
            primary = Mint,
            secondary = Violet,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        state = state,
                        onQueryChange = viewModel::updateQuery,
                        onSearch = viewModel::refreshCatalog,
                        onRetry = viewModel::refreshCatalog,
                        onSongClick = { song ->
                            viewModel.selectSong(song)
                            navController.navigate("player/${UriEncoder.encode(song.id)}")
                        },
                        onClearMessage = viewModel::clearMessage,
                    )
                }
                composable(
                    route = "player/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.StringType }),
                ) { entry ->
                    val songId = entry.arguments?.getString("songId")
                    val song = remember(songId, state.demoSongs, state.catalogSongs) {
                        viewModel.songById(songId)
                    }
                    if (song == null) {
                        MissingSongScreen(onBack = { navController.popBackStack() })
                    } else {
                        PlayerScreen(
                            song = song,
                            isPlaying = state.selectedSongId == song.id && state.isPlaying,
                            message = state.message,
                            onBack = { navController.popBackStack() },
                            onToggle = { viewModel.togglePlayback(song) },
                            onClearMessage = viewModel::clearMessage,
                        )
                    }
                }
            }
        }
    }
}

private object UriEncoder {
    fun encode(value: String): String = android.net.Uri.encode(value)
}

@Composable
private fun HomeScreen(
    state: MusicUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onSongClick: (Song) -> Unit,
    onClearMessage: () -> Unit,
) {
    Scaffold(
        containerColor = Ink,
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Mint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.GraphicEq, null, tint = Mint)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "SOUNDWAVE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.4.sp,
                    )
                }
                Spacer(Modifier.height(26.dp))
                Text(
                    "Find your\nfrequency.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 43.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text("A little space for the songs you love.", color = Muted)
                Spacer(Modifier.height(22.dp))
            }

            item {
                SectionHeader("YOUR PLAYLIST", "5 tracks")
                Spacer(Modifier.height(12.dp))
            }

            items(state.demoSongs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isPlaying = state.selectedSongId == song.id && state.isPlaying,
                    onClick = { onSongClick(song) },
                )
                Spacer(Modifier.height(10.dp))
            }

            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader("DISCOVER", "iTunes Store")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search songs or artists") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search catalog", tint = Mint)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Mint)
                    }
                }
            } else if (state.message != null && state.catalogSongs.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Panel)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(state.message, color = Color(0xFFFFB4A9))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = onRetry) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Try again")
                            }
                            androidx.compose.material3.TextButton(onClick = onClearMessage) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            } else if (state.catalogSongs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Panel)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(state.message ?: "Search the catalog to discover music.", color = Muted)
                        Button(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Try again")
                        }
                        if (state.message != null) {
                            androidx.compose.material3.TextButton(onClick = onClearMessage) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            } else {
                items(state.catalogSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isPlaying = state.selectedSongId == song.id && state.isPlaying,
                        onClick = { onSongClick(song) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Catalog metadata and artwork are provided by the iTunes Search API. " +
                        "Catalog tracks open their store page; only the bundled CC0 track is playable in-app.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp)
        Text(trailing, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SongRow(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(song = song, modifier = Modifier.size(58.dp))
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(song.artist, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Playing" else "Open ${song.title}",
            tint = Mint,
        )
    }
}

@Composable
private fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    message: String?,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onClearMessage: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF222034), Ink, Ink),
                ),
            )
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to songs")
            }
            Spacer(Modifier.weight(1f))
            Text("NOW PLAYING", color = Muted, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.8.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AlbumArt(song = song, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            Spacer(Modifier.height(32.dp))
            Text(song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(song.artist, color = Muted, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(18.dp))
            if (song.localAudioResource != null) {
                Text(
                    "Licensed CC0 audio included with the app",
                    color = Mint,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "This catalog result has no bundled audio preview.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                song.storeUrl?.let { storeUrl ->
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            try {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        storeUrl.toUri(),
                                    ),
                                )
                            } catch (error: android.content.ActivityNotFoundException) {
                                android.widget.Toast.makeText(
                                    context,
                                    "No browser is available to open this store page.",
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open store page")
                    }
                }
            }
            if (message != null) {
                Spacer(Modifier.height(14.dp))
                Text(message, color = Color(0xFFFFB4A9), style = MaterialTheme.typography.bodySmall)
                androidx.compose.material3.TextButton(onClick = onClearMessage) {
                    Text("Dismiss")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onToggle,
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(38.dp),
                )
            }
        }
    }
}

@Composable
private fun AlbumArt(song: Song, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        if (song.isLocal) Color(0xFF587B6B) else Color(0xFF6A5FA8),
                        if (song.isLocal) Color(0xFF91C8B1) else Color(0xFF8F8AE4),
                        Color(0xFF25283B),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = if (song.artworkUrl == null) "Placeholder artwork" else null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(if (song.isLocal) 78.dp else 32.dp),
        )
        song.artworkUrl?.let { artworkUrl ->
            AsyncImage(
                model = artworkUrl,
                contentDescription = "Artwork for ${song.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun MissingSongScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("That song is no longer in the list.", color = Muted)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back to songs") }
    }
}
