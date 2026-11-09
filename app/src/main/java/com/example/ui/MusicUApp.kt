package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Song
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AudioInfoDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsSheet
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.QueueSheet
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MusicUTheme
import com.example.viewmodel.MainTab
import com.example.viewmodel.MusicPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicUApp(
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()
    val artworkAccentEnabled by viewModel.artworkAccentEnabled.collectAsState()

    val currentTab by viewModel.currentTab.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val sortedSongs by viewModel.sortedLibrarySongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchCategory by viewModel.searchCategory.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val libraryTab by viewModel.libraryTab.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val currentQueue by viewModel.currentQueue.collectAsState()

    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isQueueOpen by viewModel.isQueueOpen.collectAsState()
    val isLyricsOpen by viewModel.isLyricsOpen.collectAsState()
    val isAudioInfoOpen by viewModel.isAudioInfoOpen.collectAsState()
    val songToAddToPlaylist by viewModel.songToAddToPlaylist.collectAsState()
    val isCreatePlaylistDialogOpen by viewModel.isCreatePlaylistDialogOpen.collectAsState()

    // Determine current track custom accent
    val currentAccent = if (artworkAccentEnabled && playbackState.currentSong != null) {
        Color(playbackState.currentSong!!.accentColorHex)
    } else null

    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lyricsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    MusicUTheme(
        themeMode = themeMode,
        dynamicColor = dynamicColorEnabled,
        customAccent = currentAccent
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (!isNowPlayingExpanded && selectedPlaylist == null && !isSettingsOpen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Persistent Mini Player above bottom navigation
                        MiniPlayer(
                            playbackState = playbackState,
                            onTap = { viewModel.openNowPlaying() },
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.next() }
                        )

                        // Material You Navigation Bar
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            NavigationBarItem(
                                selected = currentTab == MainTab.HOME,
                                onClick = { viewModel.setTab(MainTab.HOME) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = { Text("Home") },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("nav_item_home")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.LIBRARY,
                                onClick = { viewModel.setTab(MainTab.LIBRARY) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.LIBRARY) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                        contentDescription = "Library"
                                    )
                                },
                                label = { Text("Library") },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("nav_item_library")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.SEARCH,
                                onClick = { viewModel.setTab(MainTab.SEARCH) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                label = { Text("Search") },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("nav_item_search")
                            )

                            NavigationBarItem(
                                selected = currentTab == MainTab.PLAYLISTS,
                                onClick = { viewModel.setTab(MainTab.PLAYLISTS) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.PLAYLISTS) Icons.Filled.QueueMusic else Icons.Outlined.QueueMusic,
                                        contentDescription = "Playlists"
                                    )
                                },
                                label = { Text("Playlists") },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("nav_item_playlists")
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main Tab Screens
                when {
                    isSettingsOpen -> {
                        SettingsScreen(
                            currentThemeMode = themeMode,
                            dynamicColorEnabled = dynamicColorEnabled,
                            artworkAccentEnabled = artworkAccentEnabled,
                            onThemeModeChange = { viewModel.setThemeMode(it) },
                            onDynamicColorToggle = { viewModel.setDynamicColor(it) },
                            onArtworkAccentToggle = { viewModel.setArtworkAccent(it) },
                            onBack = { viewModel.setSettingsOpen(false) }
                        )
                    }

                    selectedPlaylist != null -> {
                        PlaylistDetailScreen(
                            playlist = selectedPlaylist!!,
                            allSongs = allSongs,
                            playbackState = playbackState,
                            onBack = { viewModel.closePlaylistDetail() },
                            onPlayAll = { songs, shuffle ->
                                viewModel.playPlaylist(selectedPlaylist!!, shuffle)
                            },
                            onSongClick = { viewModel.playSong(it) },
                            onFavoriteClick = { viewModel.toggleFavorite(it) },
                            onPlayNext = { viewModel.playNext(it) },
                            onAddToQueue = { viewModel.addToQueue(it) },
                            onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                            onRemoveFromPlaylist = { plId, songId ->
                                viewModel.removeSongFromPlaylist(plId, songId)
                            },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) }
                        )
                    }

                    else -> {
                        when (currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    allSongs = allSongs,
                                    playlists = playlists,
                                    recentlyPlayed = recentlyPlayed,
                                    playbackState = playbackState,
                                    onSongClick = { viewModel.playSong(it) },
                                    onPlayPause = { viewModel.togglePlayPause() },
                                    onPlaylistClick = { viewModel.openPlaylistDetail(it) },
                                    onSearchClick = { viewModel.setTab(MainTab.SEARCH) },
                                    onSettingsClick = { viewModel.setSettingsOpen(true) },
                                    onCreatePlaylistClick = { viewModel.setCreatePlaylistDialogOpen(true) }
                                )
                            }

                            MainTab.LIBRARY -> {
                                LibraryScreen(
                                    songs = sortedSongs,
                                    currentTab = libraryTab,
                                    currentSort = sortOption,
                                    playbackState = playbackState,
                                    onTabSelected = { viewModel.setLibraryTab(it) },
                                    onSortSelected = { viewModel.setSortOption(it) },
                                    onSongClick = { viewModel.playSong(it) },
                                    onPlayAll = { songs, shuffle ->
                                        viewModel.playQueue(if (shuffle) songs.shuffled() else songs, 0)
                                    },
                                    onFavoriteClick = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) },
                                    onImportAudio = { viewModel.handleAudioImport(it) }
                                )
                            }

                            MainTab.SEARCH -> {
                                SearchScreen(
                                    query = searchQuery,
                                    category = searchCategory,
                                    searchResults = searchResults,
                                    recentSearches = recentSearches,
                                    playbackState = playbackState,
                                    onQueryChange = { viewModel.setSearchQuery(it) },
                                    onCategoryChange = { viewModel.setSearchCategory(it) },
                                    onSearchSubmit = { viewModel.submitSearch(it) },
                                    onClearRecent = { viewModel.clearRecentSearches() },
                                    onSongClick = { viewModel.playSong(it) },
                                    onFavoriteClick = { viewModel.toggleFavorite(it) },
                                    onPlayNext = { viewModel.playNext(it) },
                                    onAddToQueue = { viewModel.addToQueue(it) },
                                    onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) }
                                )
                            }

                            MainTab.PLAYLISTS -> {
                                PlaylistsScreen(
                                    playlists = playlists,
                                    onPlaylistClick = { viewModel.openPlaylistDetail(it) },
                                    onCreatePlaylistClick = { viewModel.setCreatePlaylistDialogOpen(true) },
                                    onRenamePlaylist = { id, name -> viewModel.renamePlaylist(id, name) },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it) }
                                )
                            }
                        }
                    }
                }

                // Full Now Playing Screen Animation Overlay
                AnimatedVisibility(
                    visible = isNowPlayingExpanded && playbackState.currentSong != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    NowPlayingScreen(
                        playbackState = playbackState,
                        artworkAccentEnabled = artworkAccentEnabled,
                        onMinimize = { viewModel.closeNowPlaying() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        onSeek = { viewModel.seekTo(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeat() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onSetVolume = { viewModel.setVolume(it) },
                        onToggleMute = { viewModel.toggleMute() },
                        onOpenQueue = { viewModel.setQueueOpen(true) },
                        onOpenLyrics = { viewModel.setLyricsOpen(true) },
                        onOpenAudioInfo = { viewModel.setAudioInfoOpen(true) },
                        onAddToPlaylist = { viewModel.setSongToAddToPlaylist(it) }
                    )
                }

                // Dialogs & Sheets
                if (isQueueOpen) {
                    QueueSheet(
                        queue = currentQueue,
                        playbackState = playbackState,
                        onDismiss = { viewModel.setQueueOpen(false) },
                        onSongClick = { idx ->
                            viewModel.playerManager.playSongAtIndex(idx, true)
                        },
                        onRemoveSong = { idx -> viewModel.removeFromQueue(idx) },
                        onClearQueue = { viewModel.clearQueue() },
                        sheetState = queueSheetState
                    )
                }

                if (isLyricsOpen && playbackState.currentSong != null) {
                    LyricsSheet(
                        song = playbackState.currentSong!!,
                        playbackState = playbackState,
                        onDismiss = { viewModel.setLyricsOpen(false) },
                        onSeekTo = { viewModel.seekTo(it) },
                        sheetState = lyricsSheetState
                    )
                }

                if (isAudioInfoOpen && playbackState.currentSong != null) {
                    AudioInfoDialog(
                        song = playbackState.currentSong!!,
                        onDismiss = { viewModel.setAudioInfoOpen(false) }
                    )
                }

                if (isCreatePlaylistDialogOpen) {
                    CreatePlaylistDialog(
                        onDismiss = { viewModel.setCreatePlaylistDialogOpen(false) },
                        onConfirm = { name, desc -> viewModel.createPlaylist(name, desc) }
                    )
                }

                if (songToAddToPlaylist != null) {
                    AddToPlaylistDialog(
                        song = songToAddToPlaylist!!,
                        playlists = playlists.filter { !it.isFavoritesPlaylist },
                        onDismiss = { viewModel.setSongToAddToPlaylist(null) },
                        onPlaylistSelected = { plId ->
                            viewModel.addSongToPlaylist(plId, songToAddToPlaylist!!.id)
                        }
                    )
                }
            }
        }
    }
}
