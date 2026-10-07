package com.dcsxgdg.soundwave

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dcsxgdg.soundwave.data.DemoSongs
import com.dcsxgdg.soundwave.data.ITunesRepository
import com.dcsxgdg.soundwave.data.Song
import com.dcsxgdg.soundwave.playback.PlaybackController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

data class MusicUiState(
    val demoSongs: List<Song> = DemoSongs.songs,
    val catalogSongs: List<Song> = emptyList(),
    val selectedSongId: String? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val query: String = "instrumental music",
    val message: String? = null,
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ITunesRepository()
    private val playbackController = PlaybackController(application)
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState = _uiState.asStateFlow()
    private var catalogSearch: Job? = null

    init {
        viewModelScope.launch {
            playbackController.state.collect { playback ->
                _uiState.value = _uiState.value.copy(
                    selectedSongId = playback.songId ?: _uiState.value.selectedSongId,
                    isPlaying = playback.isPlaying,
                    message = playback.error ?: _uiState.value.message,
                )
            }
        }
        refreshCatalog()
    }

    fun selectSong(song: Song) {
        if (playbackController.state.value.songId != null &&
            playbackController.state.value.songId != song.id
        ) {
            playbackController.stop()
        }
        _uiState.value = _uiState.value.copy(selectedSongId = song.id, message = null)
    }

    fun togglePlayback(song: Song) {
        playbackController.toggle(song)
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun refreshCatalog() {
        val query = _uiState.value.query.trim().ifBlank { "instrumental music" }
        catalogSearch?.cancel()
        catalogSearch = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            try {
                val songs = repository.searchSongs(query)
                _uiState.value = _uiState.value.copy(
                    catalogSongs = songs,
                    isLoading = false,
                    message = if (songs.isEmpty()) {
                        "No songs matched that search. Try a different phrase."
                    } else {
                        null
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Couldn't load the music catalog: ${error.message ?: "unknown error"}",
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun songById(id: String?): Song? =
        (_uiState.value.demoSongs + _uiState.value.catalogSongs).firstOrNull { it.id == id }

    override fun onCleared() {
        playbackController.release()
        super.onCleared()
    }
}
