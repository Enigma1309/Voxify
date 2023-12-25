package com.exilonium.voxify.preferences

import com.exilonium.voxify.GlobalPreferencesHolder
import com.exilonium.voxify.enums.AlbumSortBy
import com.exilonium.voxify.enums.ArtistSortBy
import com.exilonium.voxify.enums.PlaylistSortBy
import com.exilonium.voxify.enums.SongSortBy
import com.exilonium.voxify.enums.SortOrder

object OrderPreferences : GlobalPreferencesHolder() {
    var songSortOrder by enum(SortOrder.Descending)
    var localSongSortOrder by enum(SortOrder.Descending)
    var playlistSortOrder by enum(SortOrder.Descending)
    var albumSortOrder by enum(SortOrder.Descending)
    var artistSortOrder by enum(SortOrder.Descending)

    var songSortBy by enum(SongSortBy.DateAdded)
    var localSongSortBy by enum(SongSortBy.DateAdded)
    var playlistSortBy by enum(PlaylistSortBy.DateAdded)
    var albumSortBy by enum(AlbumSortBy.DateAdded)
    var artistSortBy by enum(ArtistSortBy.DateAdded)
}
