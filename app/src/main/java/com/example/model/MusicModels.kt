package com.example.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtRes: Int? = null,
    val albumArtUri: String? = null,
    val genre: String = "Lo-Fi",
    val accentColorHex: Long = 0xFF42A5F5,
    val secondaryAccentHex: Long = 0xFFEC407A,
    val audioPath: String = "",
    val isImported: Boolean = false,
    val isFavorite: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val format: String = "FLAC / 44.1 kHz",
    val bitrate: String = "320 kbps"
)

data class Playlist(
    val id: String,
    val title: String,
    val description: String,
    val coverResId: Int? = null,
    val songIds: List<String> = emptyList(),
    val isFavoritesPlaylist: Boolean = false
)

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class SortOption(val displayName: String) {
    RECENT("Recently Added"),
    TITLE("Song Name"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration")
}

enum class LibraryTab(val displayName: String) {
    ALL_SONGS("All Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    RECENT("Recent")
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val volume: Float = 0.85f,
    val isMuted: Boolean = false
)
