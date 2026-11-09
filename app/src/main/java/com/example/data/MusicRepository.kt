package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.model.LyricLine
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class MusicRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("musicu_prefs", Context.MODE_PRIVATE)

    private val defaultSongs = listOf(
        Song(
            id = "default_1",
            title = "Rainy Neon Nights",
            artist = "Lofi Dreamer ft. City Lights",
            album = "Midnight Reveries Vol. 1",
            durationMs = 32000L,
            albumArtRes = R.drawable.cover_midnight_city,
            genre = "Lo-Fi Beats",
            accentColorHex = 0xFF26C6DA, // Cyan Teal
            secondaryAccentHex = 0xFFFFA726, // Warm Amber
            audioPath = "rainy_nights",
            lyrics = listOf(
                LyricLine(0L, "[Music intro • Smooth vinyl crackle & warm electric piano]"),
                LyricLine(4000L, "Raindrops tapping on the windowsill tonight"),
                LyricLine(8000L, "Reflections of neon dancing in the amber light"),
                LyricLine(12000L, "Quiet moments as the city goes to sleep"),
                LyricLine(16000L, "Melodies and memories that we choose to keep"),
                LyricLine(20000L, "[Smooth jazzy chord progression]"),
                LyricLine(24000L, "Your music. Your mood. Your universe."),
                LyricLine(28000L, "Midnight whispers in the cool night breeze...")
            ),
            format = "FLAC 24-bit / 44.1 kHz",
            bitrate = "920 kbps"
        ),
        Song(
            id = "default_2",
            title = "Neon Horizon",
            artist = "RetroWave Pulse",
            album = "Synthwave City Odyssey",
            durationMs = 32000L,
            albumArtRes = R.drawable.cover_sunset_drive,
            genre = "Synthwave",
            accentColorHex = 0xFFEC407A, // Neon Pink
            secondaryAccentHex = 0xFFAB47BC, // Purple Glow
            audioPath = "neon_horizon",
            lyrics = listOf(
                LyricLine(0L, "[Synthesizer pulse begins • 80s analog bass]"),
                LyricLine(4000L, "Driving down the highway into the crimson sunset"),
                LyricLine(8000L, "Electric dreams that we won't forget"),
                LyricLine(12000L, "Neon horizons glowing in the distance ahead"),
                LyricLine(16000L, "Follow the retro groove where the light has led"),
                LyricLine(20000L, "[Arpeggiated lead solo]"),
                LyricLine(24000L, "Feel the rhythm of the modern beat"),
                LyricLine(28000L, "Synthwave vibes taking over the street...")
            ),
            format = "WAV 16-bit / 44.1 kHz",
            bitrate = "1411 kbps"
        ),
        Song(
            id = "default_3",
            title = "Emerald Aurora",
            artist = "Aetheris Synthesis",
            album = "Cosmic Sanctuary",
            durationMs = 32000L,
            albumArtRes = R.drawable.cover_emerald_aurora,
            genre = "Ambient Electronic",
            accentColorHex = 0xFF26A69A, // Emerald Cyan
            secondaryAccentHex = 0xFF80CBC4, // Pale Mint
            audioPath = "emerald_aurora",
            lyrics = listOf(
                LyricLine(0L, "[Tranquil celestial drone and shimmering harmonic bells]"),
                LyricLine(6000L, "Ribbons of green dancing across the polar sky"),
                LyricLine(12000L, "Tranquil stillness as constellations drift by"),
                LyricLine(18000L, "Deep harmonic resonance filling the space"),
                LyricLine(24000L, "A universe of calm in this peaceful place"),
                LyricLine(28000L, "[Subtle celestial shimmer fade]")
            ),
            format = "Hi-Res Audio / 96 kHz",
            bitrate = "2304 kbps"
        ),
        Song(
            id = "default_4",
            title = "Morning Espresso",
            artist = "Acoustic Aura",
            album = "Sunday Morning Sessions",
            durationMs = 32000L,
            albumArtRes = R.drawable.cover_musicu_icon,
            genre = "Acoustic Chill",
            accentColorHex = 0xFFFF7043, // Sunset Orange
            secondaryAccentHex = 0xFFFFD54F, // Warm Gold
            audioPath = "morning_espresso",
            lyrics = listOf(
                LyricLine(0L, "[Acoustic guitar arpeggio and fingerstyle rhythm]"),
                LyricLine(4000L, "Fresh brewed coffee and morning golden sun"),
                LyricLine(8000L, "A brand new day has gently begun"),
                LyricLine(12000L, "Turn the page and let the melody play"),
                LyricLine(16000L, "MusicU soundtrack guiding the way"),
                LyricLine(20000L, "[Harmonic acoustic guitar groove]"),
                LyricLine(24000L, "Warm coffee cups and mellow peace"),
                LyricLine(28000L, "Where all everyday worries cease...")
            ),
            format = "MP3 / 320 kbps",
            bitrate = "320 kbps"
        )
    )

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(true)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _artworkAccentEnabled = MutableStateFlow(true)
    val artworkAccentEnabled: StateFlow<Boolean> = _artworkAccentEnabled.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Load theme preferences
        val themePref = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        _themeMode.value = try { ThemeMode.valueOf(themePref) } catch (e: Exception) { ThemeMode.SYSTEM }
        _dynamicColorEnabled.value = prefs.getBoolean("dynamic_color", true)
        _artworkAccentEnabled.value = prefs.getBoolean("artwork_accent", true)

        // Load imported songs
        val imported = loadImportedSongs()
        val allSongs = defaultSongs + imported
        _songs.value = allSongs

        // Load playlists
        loadPlaylists(allSongs)

        // Load recent searches
        val searchesJson = prefs.getString("recent_searches", "[]") ?: "[]"
        val searchesList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(searchesJson)
            for (i in 0 until jsonArr.length()) {
                searchesList.add(jsonArr.getString(i))
            }
        } catch (e: Exception) {
            searchesList.addAll(listOf("Lofi", "Synthwave", "Chill", "Ambient"))
        }
        _recentSearches.value = searchesList.ifEmpty { listOf("Lofi", "Synthwave", "Chill", "Ambient") }
    }

    private fun loadPlaylists(allSongs: List<Song>) {
        val storedJson = prefs.getString("playlists", null)
        val list = mutableListOf<Playlist>()

        if (!storedJson.isNullOrEmpty()) {
            try {
                val jsonArr = JSONArray(storedJson)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val songIdsArr = obj.getJSONArray("songIds")
                    val songIds = mutableListOf<String>()
                    for (j in 0 until songIdsArr.length()) {
                        songIds.add(songIdsArr.getString(j))
                    }
                    list.add(
                        Playlist(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            coverResId = if (obj.has("coverResId")) obj.getInt("coverResId") else null,
                            songIds = songIds,
                            isFavoritesPlaylist = obj.optBoolean("isFavorites", false)
                        )
                    )
                }
            } catch (e: Exception) {
                list.clear()
            }
        }

        if (list.isEmpty()) {
            // Seed initial playlists
            val favorites = Playlist(
                id = "pl_favorites",
                title = "Favorites",
                description = "Songs you have loved and saved.",
                coverResId = R.drawable.cover_midnight_city,
                songIds = listOf("default_1", "default_2"),
                isFavoritesPlaylist = true
            )
            val dailyMix = Playlist(
                id = "pl_daily_mix",
                title = "Daily Mix",
                description = "Curated blend of lo-fi and chillwave beats.",
                coverResId = R.drawable.cover_sunset_drive,
                songIds = listOf("default_1", "default_2", "default_3")
            )
            val chillVibes = Playlist(
                id = "pl_chill_vibes",
                title = "Chill Vibes",
                description = "Relaxing melodies to unwind and drift away.",
                coverResId = R.drawable.cover_emerald_aurora,
                songIds = listOf("default_1", "default_3", "default_4")
            )
            val focusMusic = Playlist(
                id = "pl_focus",
                title = "Focus & Flow",
                description = "Ambient soundscapes for deep concentration.",
                coverResId = R.drawable.cover_musicu_icon,
                songIds = listOf("default_3", "default_4")
            )
            list.addAll(listOf(favorites, dailyMix, chillVibes, focusMusic))
            savePlaylistsToDisk(list)
        }

        _playlists.value = list

        // Update favorite flags on songs
        val favoriteIds = list.find { it.isFavoritesPlaylist }?.songIds ?: emptyList()
        _songs.update { current ->
            current.map { song ->
                song.copy(isFavorite = favoriteIds.contains(song.id))
            }
        }
    }

    private fun loadImportedSongs(): List<Song> {
        val jsonStr = prefs.getString("imported_songs", "[]") ?: "[]"
        val list = mutableListOf<Song>()
        try {
            val jsonArr = JSONArray(jsonStr)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                list.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", "Local Audio"),
                        durationMs = obj.optLong("durationMs", 180000L),
                        genre = obj.optString("genre", "Imported"),
                        audioPath = obj.getString("audioPath"),
                        isImported = true,
                        format = obj.optString("format", "Local Audio"),
                        bitrate = obj.optString("bitrate", "Variable")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addImportedSong(song: Song) {
        val current = _songs.value
        val updated = current + song
        _songs.value = updated

        // Persist to SharedPreferences
        val importedList = updated.filter { it.isImported }
        val jsonArr = JSONArray()
        for (s in importedList) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album)
                put("durationMs", s.durationMs)
                put("genre", s.genre)
                put("audioPath", s.audioPath)
                put("format", s.format)
                put("bitrate", s.bitrate)
            }
            jsonArr.put(obj)
        }
        prefs.edit().putString("imported_songs", jsonArr.toString()).apply()
    }

    fun toggleFavorite(songId: String) {
        val currentSongs = _songs.value
        val target = currentSongs.find { it.id == songId } ?: return
        val newFav = !target.isFavorite

        _songs.update { list ->
            list.map { if (it.id == songId) it.copy(isFavorite = newFav) else it }
        }

        // Update in Favorites playlist
        val currentPlaylists = _playlists.value.toMutableList()
        val favPlaylistIdx = currentPlaylists.indexOfFirst { it.isFavoritesPlaylist }
        if (favPlaylistIdx != -1) {
            val favPlaylist = currentPlaylists[favPlaylistIdx]
            val newSongIds = favPlaylist.songIds.toMutableList()
            if (newFav) {
                if (!newSongIds.contains(songId)) newSongIds.add(0, songId)
            } else {
                newSongIds.remove(songId)
            }
            currentPlaylists[favPlaylistIdx] = favPlaylist.copy(songIds = newSongIds)
            _playlists.value = currentPlaylists
            savePlaylistsToDisk(currentPlaylists)
        }
    }

    fun createPlaylist(title: String, description: String = "") {
        val newPlaylist = Playlist(
            id = "pl_${System.currentTimeMillis()}",
            title = title,
            description = description,
            coverResId = R.drawable.cover_midnight_city,
            songIds = emptyList()
        )
        val updated = _playlists.value + newPlaylist
        _playlists.value = updated
        savePlaylistsToDisk(updated)
    }

    fun renamePlaylist(id: String, newTitle: String) {
        val updated = _playlists.value.map {
            if (it.id == id && !it.isFavoritesPlaylist) it.copy(title = newTitle) else it
        }
        _playlists.value = updated
        savePlaylistsToDisk(updated)
    }

    fun deletePlaylist(id: String) {
        val updated = _playlists.value.filter { it.id != id || it.isFavoritesPlaylist }
        _playlists.value = updated
        savePlaylistsToDisk(updated)
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId && !pl.songIds.contains(songId)) {
                pl.copy(songIds = pl.songIds + songId)
            } else pl
        }
        _playlists.value = updated
        savePlaylistsToDisk(updated)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(songIds = pl.songIds.filter { it != songId })
            } else pl
        }
        _playlists.value = updated
        savePlaylistsToDisk(updated)
    }

    fun recordRecentlyPlayed(song: Song) {
        val current = _recentlyPlayed.value.toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        _recentlyPlayed.value = current.take(15)
    }

    fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        val updated = current.take(10)
        _recentSearches.value = updated
        val jsonArr = JSONArray(updated)
        prefs.edit().putString("recent_searches", jsonArr.toString()).apply()
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().putString("recent_searches", "[]").apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
    }

    fun setArtworkAccent(enabled: Boolean) {
        _artworkAccentEnabled.value = enabled
        prefs.edit().putBoolean("artwork_accent", enabled).apply()
    }

    private fun savePlaylistsToDisk(list: List<Playlist>) {
        val jsonArr = JSONArray()
        for (pl in list) {
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("title", pl.title)
                put("description", pl.description)
                pl.coverResId?.let { put("coverResId", it) }
                put("isFavorites", pl.isFavoritesPlaylist)
                val songsArr = JSONArray(pl.songIds)
                put("songIds", songsArr)
            }
            jsonArr.put(obj)
        }
        prefs.edit().putString("playlists", jsonArr.toString()).apply()
    }
}
