package com.example.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.data.MusicRepository
import com.example.model.LibraryTab
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.model.SortOption
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
    HOME, LIBRARY, SEARCH, PLAYLISTS
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val playerManager = AudioPlayerManager(application, viewModelScope)

    // Navigation & Sheet states
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isQueueOpen = MutableStateFlow(false)
    val isQueueOpen: StateFlow<Boolean> = _isQueueOpen.asStateFlow()

    private val _isLyricsOpen = MutableStateFlow(false)
    val isLyricsOpen: StateFlow<Boolean> = _isLyricsOpen.asStateFlow()

    private val _isAudioInfoOpen = MutableStateFlow(false)
    val isAudioInfoOpen: StateFlow<Boolean> = _isAudioInfoOpen.asStateFlow()

    private val _songToAddToPlaylist = MutableStateFlow<Song?>(null)
    val songToAddToPlaylist: StateFlow<Song?> = _songToAddToPlaylist.asStateFlow()

    private val _isCreatePlaylistDialogOpen = MutableStateFlow(false)
    val isCreatePlaylistDialogOpen: StateFlow<Boolean> = _isCreatePlaylistDialogOpen.asStateFlow()

    // Library tab & sorting
    private val _libraryTab = MutableStateFlow(LibraryTab.ALL_SONGS)
    val libraryTab: StateFlow<LibraryTab> = _libraryTab.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RECENT)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Search query & category
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow("All")
    val searchCategory: StateFlow<String> = _searchCategory.asStateFlow()

    // Expose repository flows
    val allSongs: StateFlow<List<Song>> = repository.songs
    val playlists: StateFlow<List<Playlist>> = repository.playlists
    val recentlyPlayed: StateFlow<List<Song>> = repository.recentlyPlayed
    val recentSearches: StateFlow<List<String>> = repository.recentSearches
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
    val dynamicColorEnabled: StateFlow<Boolean> = repository.dynamicColorEnabled
    val artworkAccentEnabled: StateFlow<Boolean> = repository.artworkAccentEnabled

    // Expose playback flows
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val currentQueue: StateFlow<List<Song>> = playerManager.currentQueue

    // Sorted library songs
    val sortedLibrarySongs: StateFlow<List<Song>> = combine(allSongs, sortOption) { songs, sort ->
        when (sort) {
            SortOption.RECENT -> songs
            SortOption.TITLE -> songs.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> songs.sortedBy { it.album.lowercase() }
            SortOption.DURATION -> songs.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Search results flow
    val searchResults: StateFlow<List<Song>> = combine(allSongs, searchQuery, searchCategory) { songs, query, category ->
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            songs.filter { song ->
                when (category) {
                    "Songs" -> song.title.lowercase().contains(q)
                    "Artists" -> song.artist.lowercase().contains(q)
                    "Albums" -> song.album.lowercase().contains(q)
                    else -> song.title.lowercase().contains(q) ||
                            song.artist.lowercase().contains(q) ||
                            song.album.lowercase().contains(q) ||
                            song.genre.lowercase().contains(q)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Set initial queue with default songs
        viewModelScope.launch {
            allSongs.collect { list ->
                if (playerManager.currentQueue.value.isEmpty() && list.isNotEmpty()) {
                    playerManager.setQueue(list, startIndex = 0, startPlaying = false)
                }
            }
        }
    }

    // Playback control delegates
    fun playSong(song: Song) {
        repository.recordRecentlyPlayed(song)
        playerManager.playSong(song)
        _isNowPlayingExpanded.value = true
    }

    fun playPlaylist(playlist: Playlist, shuffle: Boolean = false) {
        val songsMap = allSongs.value.associateBy { it.id }
        val playlistSongs = playlist.songIds.mapNotNull { songsMap[it] }
        if (playlistSongs.isNotEmpty()) {
            val songsToPlay = if (shuffle) playlistSongs.shuffled() else playlistSongs
            playerManager.setQueue(songsToPlay, startIndex = 0, startPlaying = true)
            repository.recordRecentlyPlayed(songsToPlay.first())
            _isNowPlayingExpanded.value = true
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isNotEmpty() && startIndex in songs.indices) {
            repository.recordRecentlyPlayed(songs[startIndex])
            playerManager.setQueue(songs, startIndex, startPlaying = true)
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun toggleRepeat() = playerManager.toggleRepeat()
    fun setVolume(vol: Float) = playerManager.setVolume(vol)
    fun toggleMute() = playerManager.toggleMute()
    fun playNext(song: Song) = playerManager.playNext(song)
    fun addToQueue(song: Song) = playerManager.addToQueue(song)
    fun removeFromQueue(index: Int) = playerManager.removeFromQueue(index)
    fun clearQueue() = playerManager.clearQueue()

    // Navigation and sheets
    fun setTab(tab: MainTab) {
        _currentTab.value = tab
        _selectedPlaylist.value = null
    }

    fun openNowPlaying() {
        _isNowPlayingExpanded.value = true
    }

    fun closeNowPlaying() {
        _isNowPlayingExpanded.value = false
    }

    fun openPlaylistDetail(playlist: Playlist) {
        _selectedPlaylist.value = playlist
    }

    fun closePlaylistDetail() {
        _selectedPlaylist.value = null
    }

    fun setSettingsOpen(open: Boolean) {
        _isSettingsOpen.value = open
    }

    fun setQueueOpen(open: Boolean) {
        _isQueueOpen.value = open
    }

    fun setLyricsOpen(open: Boolean) {
        _isLyricsOpen.value = open
    }

    fun setAudioInfoOpen(open: Boolean) {
        _isAudioInfoOpen.value = open
    }

    fun setSongToAddToPlaylist(song: Song?) {
        _songToAddToPlaylist.value = song
    }

    fun setCreatePlaylistDialogOpen(open: Boolean) {
        _isCreatePlaylistDialogOpen.value = open
    }

    fun setLibraryTab(tab: LibraryTab) {
        _libraryTab.value = tab
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchCategory(category: String) {
        _searchCategory.value = category
    }

    fun submitSearch(query: String) {
        _searchQuery.value = query
        repository.addRecentSearch(query)
    }

    fun clearRecentSearches() = repository.clearRecentSearches()

    // Playlist and song operations
    fun toggleFavorite(songId: String) = repository.toggleFavorite(songId)

    fun createPlaylist(name: String, desc: String = "") {
        repository.createPlaylist(name, desc)
        _isCreatePlaylistDialogOpen.value = false
    }

    fun renamePlaylist(id: String, newName: String) = repository.renamePlaylist(id, newName)

    fun deletePlaylist(id: String) {
        if (_selectedPlaylist.value?.id == id) {
            _selectedPlaylist.value = null
        }
        repository.deletePlaylist(id)
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        repository.addSongToPlaylist(playlistId, songId)
        _songToAddToPlaylist.value = null
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        repository.removeSongFromPlaylist(playlistId, songId)
        _selectedPlaylist.update { current ->
            if (current?.id == playlistId) {
                current.copy(songIds = current.songIds.filter { it != songId })
            } else current
        }
    }

    // Settings
    fun setThemeMode(mode: ThemeMode) = repository.setThemeMode(mode)
    fun setDynamicColor(enabled: Boolean) = repository.setDynamicColor(enabled)
    fun setArtworkAccent(enabled: Boolean) = repository.setArtworkAccent(enabled)

    // Import audio from device URI
    fun handleAudioImport(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                var displayName = "Imported Track"
                var duration = 180000L
                var artist = "Local Artist"
                var album = "Imported Music"

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIdx) ?: "Imported Track"
                    }
                }

                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                        if (it.isNotBlank()) displayName = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                        if (it.isNotBlank()) artist = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                        if (it.isNotBlank()) album = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                        if (it > 0) duration = it
                    }
                    retriever.release()
                } catch (e: Exception) {
                    // Fallback to filename
                }

                val cleanTitle = displayName.substringBeforeLast(".")
                val importedSong = Song(
                    id = "imported_${System.currentTimeMillis()}",
                    title = cleanTitle,
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    genre = "Local Audio",
                    accentColorHex = 0xFF42A5F5,
                    secondaryAccentHex = 0xFF26A69A,
                    audioPath = uri.toString(),
                    isImported = true,
                    format = "Local File",
                    bitrate = "Native Quality"
                )

                repository.addImportedSong(importedSong)
                playSong(importedSong)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.destroy()
    }
}
