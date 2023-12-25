package com.exilonium.voxify.models

import androidx.compose.runtime.Immutable

@Immutable
data class PlaylistPreview(
    val id: Long,
    val name: String,
    val songCount: Int
) {
    val playlist by lazy {
        Playlist(
            id = id,
            name = name,
            browseId = null
        )
    }
}
