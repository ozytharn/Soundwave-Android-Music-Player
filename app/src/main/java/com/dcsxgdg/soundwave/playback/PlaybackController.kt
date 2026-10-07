package com.dcsxgdg.soundwave.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dcsxgdg.soundwave.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

data class PlaybackState(
    val songId: String? = null,
    val isPlaying: Boolean = false,
    val error: String? = null,
)

class PlaybackController(context: Context) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(PlaybackState())
    val state = _state.asStateFlow()

    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)
    private var controller: MediaController? = null
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? =
        null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.value = _state.value.copy(songId = mediaItem?.mediaId, isPlaying = false)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _state.value = _state.value.copy(
                isPlaying = false,
                error = error.message ?: "Audio playback failed.",
            )
        }
    }

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    controller = future.get().also {
                        it.addListener(listener)
                        _state.value = _state.value.copy(
                            songId = it.currentMediaItem?.mediaId,
                            isPlaying = it.isPlaying,
                            error = null,
                        )
                    }
                } catch (error: ExecutionException) {
                    _state.value = _state.value.copy(
                        error = error.cause?.message ?: "Could not connect to audio service.",
                    )
                } catch (error: InterruptedException) {
                    Thread.currentThread().interrupt()
                    _state.value = _state.value.copy(error = "Audio service connection was interrupted.")
                }
            },
            mainExecutor,
        )
    }

    fun toggle(song: Song) {
        val current = controller
        if (current == null) {
            _state.value = _state.value.copy(error = "The audio service is still starting. Try again.")
            return
        }

        val resourceId = song.localAudioResource
        if (resourceId == null) {
            _state.value = _state.value.copy(
                isPlaying = false,
                error = "This catalog item has no bundled audio. Open its store page to learn more.",
            )
            return
        }

        _state.value = _state.value.copy(error = null)
        if (_state.value.songId == song.id) {
            if (current.isPlaying) {
                current.pause()
            } else {
                if (current.playbackState == Player.STATE_ENDED) current.seekToDefaultPosition()
                current.play()
            }
            return
        }

        val item = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri("android.resource://${appContext.packageName}/$resourceId".toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .apply {
                        song.artworkUrl?.let { setArtworkUri(it.toUri()) }
                    }
                    .build(),
            )
            .build()
        _state.value = PlaybackState(songId = song.id)
        current.setMediaItem(item)
        current.prepare()
        current.play()
    }

    fun stop() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        _state.value = PlaybackState()
    }

    fun release() {
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
    }
}
