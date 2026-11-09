package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.model.PlaybackState
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    private var currentIndex: Int = -1

    init {
        startProgressTracking()
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, startPlaying: Boolean = true) {
        _currentQueue.value = songs
        if (startIndex in songs.indices) {
            playSongAtIndex(startIndex, startPlaying)
        }
    }

    fun playSong(song: Song) {
        val queue = _currentQueue.value
        val index = queue.indexOfFirst { it.id == song.id }
        if (index != -1) {
            playSongAtIndex(index, true)
        } else {
            // Append or prepend to queue
            val newQueue = queue + song
            _currentQueue.value = newQueue
            playSongAtIndex(newQueue.lastIndex, true)
        }
    }

    fun playSongAtIndex(index: Int, startPlaying: Boolean = true) {
        val queue = _currentQueue.value
        if (index !in queue.indices) return

        currentIndex = index
        val song = queue[index]

        _playbackState.update {
            it.copy(
                currentSong = song,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = song.durationMs
            )
        }

        scope.launch(Dispatchers.IO) {
            try {
                releasePlayer()

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                }

                // Check audio source
                if (song.isImported && !song.audioPath.isNullOrEmpty()) {
                    val uri = Uri.parse(song.audioPath)
                    player.setDataSource(context, uri)
                } else {
                    val trackFile = AudioSynthHelper.getOrCreateTrackFile(context, song.audioPath.ifEmpty { "rainy_nights" })
                    player.setDataSource(trackFile.absolutePath)
                }

                player.setOnPreparedListener { mp ->
                    val actualDuration = if (mp.duration > 0) mp.duration.toLong() else song.durationMs
                    _playbackState.update { it.copy(durationMs = actualDuration) }
                    applyVolume()
                    if (startPlaying) {
                        mp.start()
                        _playbackState.update { it.copy(isPlaying = true) }
                    }
                }

                player.setOnCompletionListener {
                    handleTrackCompletion()
                }

                player.setOnErrorListener { _, _, _ ->
                    _playbackState.update { it.copy(isPlaying = false) }
                    true
                }

                player.prepareAsync()
                mediaPlayer = player

            } catch (e: Exception) {
                e.printStackTrace()
                _playbackState.update { it.copy(isPlaying = false) }
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: run {
            if (_currentQueue.value.isNotEmpty()) {
                playSongAtIndex(0, true)
            }
            return
        }

        try {
            if (player.isPlaying) {
                player.pause()
                _playbackState.update { it.copy(isPlaying = false) }
            } else {
                player.start()
                _playbackState.update { it.copy(isPlaying = true) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _playbackState.update { state -> state.copy(isPlaying = false) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    _playbackState.update { state -> state.copy(isPlaying = true) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun next() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return

        if (_playbackState.value.isShuffle) {
            val nextIdx = (queue.indices).random()
            playSongAtIndex(nextIdx, true)
        } else {
            val nextIdx = (currentIndex + 1) % queue.size
            playSongAtIndex(nextIdx, true)
        }
    }

    fun previous() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return

        val currentPos = _playbackState.value.currentPositionMs
        if (currentPos > 3000L) {
            // Restart current track
            seekTo(0L)
            return
        }

        val prevIdx = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
        playSongAtIndex(prevIdx, true)
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _playbackState.update { it.copy(currentPositionMs = positionMs) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _playbackState.update { it.copy(volume = clamped, isMuted = clamped == 0f) }
        applyVolume()
    }

    fun toggleMute() {
        _playbackState.update {
            val newMute = !it.isMuted
            it.copy(isMuted = newMute)
        }
        applyVolume()
    }

    private fun applyVolume() {
        val state = _playbackState.value
        val actualVol = if (state.isMuted) 0f else state.volume
        try {
            mediaPlayer?.setVolume(actualVol, actualVol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleTrackCompletion() {
        val state = _playbackState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                mediaPlayer?.start()
                _playbackState.update { it.copy(isPlaying = true) }
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                val queue = _currentQueue.value
                if (currentIndex < queue.size - 1) {
                    next()
                } else {
                    _playbackState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                }
            }
        }
    }

    fun playNext(song: Song) {
        val list = _currentQueue.value.toMutableList()
        val insertIndex = (currentIndex + 1).coerceAtMost(list.size)
        list.add(insertIndex, song)
        _currentQueue.value = list
    }

    fun addToQueue(song: Song) {
        val list = _currentQueue.value.toMutableList()
        list.add(song)
        _currentQueue.value = list
    }

    fun removeFromQueue(index: Int) {
        val list = _currentQueue.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _currentQueue.value = list
            if (index < currentIndex) {
                currentIndex--
            } else if (index == currentIndex) {
                if (list.isNotEmpty()) {
                    playSongAtIndex(index.coerceAtMost(list.size - 1), _playbackState.value.isPlaying)
                } else {
                    releasePlayer()
                    _playbackState.update { it.copy(currentSong = null, isPlaying = false) }
                }
            }
        }
    }

    fun clearQueue() {
        val current = _playbackState.value.currentSong
        if (current != null) {
            _currentQueue.value = listOf(current)
            currentIndex = 0
        } else {
            _currentQueue.value = emptyList()
            currentIndex = -1
            releasePlayer()
            _playbackState.update { it.copy(isPlaying = false) }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            val pos = player.currentPosition.toLong()
                            _playbackState.update { it.copy(currentPositionMs = pos) }
                        }
                    }
                } catch (e: Exception) {
                    // Ignored on race conditions
                }
                delay(250)
            }
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            mediaPlayer = null
        }
    }

    fun destroy() {
        progressJob?.cancel()
        releasePlayer()
    }
}
