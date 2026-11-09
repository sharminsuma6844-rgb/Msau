package com.example

import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.model.SortOption
import com.example.ui.components.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFormatDuration() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:32", formatDuration(32000L))
        assertEquals("3:45", formatDuration(225000L))
        assertEquals("10:05", formatDuration(605000L))
    }

    @Test
    fun testSongModelCreation() {
        val song = Song(
            id = "test_1",
            title = "Chill Lofi",
            artist = "MusicU Artist",
            album = "Lo-Fi Beats",
            durationMs = 180000L,
            genre = "Lo-Fi"
        )
        assertEquals("Chill Lofi", song.title)
        assertEquals("MusicU Artist", song.artist)
        assertFalse(song.isFavorite)
    }

    @Test
    fun testPlaylistCreation() {
        val playlist = Playlist(
            id = "pl_1",
            title = "Chill Vibes",
            description = "Relaxing songs",
            songIds = listOf("song_1", "song_2")
        )
        assertEquals(2, playlist.songIds.size)
        assertTrue(playlist.songIds.contains("song_1"))
    }
}
