package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Song

@Composable
fun ArtworkImage(
    song: Song?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    fallbackColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(fallbackColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            song?.albumArtRes != null -> {
                Image(
                    painter = painterResource(id = song.albumArtRes),
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            !song?.albumArtUri.isNullOrEmpty() -> {
                AsyncImage(
                    model = song?.albumArtUri,
                    contentDescription = song?.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                val accent1 = song?.let { Color(it.accentColorHex) } ?: MaterialTheme.colorScheme.primary
                val accent2 = song?.let { Color(it.secondaryAccentHex) } ?: MaterialTheme.colorScheme.tertiary
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(accent1.copy(alpha = 0.8f), accent2.copy(alpha = 0.8f)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
