package com.exilonium.voxify.utils

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.exilonium.voxify.Database
import com.exilonium.voxify.LocalPlayerServiceBinder
import com.exilonium.voxify.R
import com.exilonium.voxify.models.Format
import com.exilonium.voxify.service.PrecacheService
import com.exilonium.voxify.ui.components.themed.HeaderIconButton
import com.exilonium.voxify.ui.styling.LocalAppearance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PlaylistDownloadIcon(songs: ImmutableList<MediaItem>) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    if (!songs.all { isCached(mediaId = it.mediaId) }) HeaderIconButton(
        icon = R.drawable.download,
        color = colorPalette.text,
        onClick = {
            songs.forEach {
                PrecacheService.scheduleCache(context.applicationContext, it)
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun isCached(mediaId: String): Boolean {
    val cache = LocalPlayerServiceBinder.current?.cache ?: return false
    var format: Format? by remember { mutableStateOf(null) }

    LaunchedEffect(mediaId) {
        Database.format(mediaId).distinctUntilChanged().collect { format = it }
    }

    return format?.contentLength?.let { len ->
        cache.isCached(mediaId, 0, len)
    } ?: false
}
