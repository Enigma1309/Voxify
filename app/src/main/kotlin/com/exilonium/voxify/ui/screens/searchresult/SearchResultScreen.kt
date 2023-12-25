package com.exilonium.voxify.ui.screens.searchresult

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exilonium.compose.persist.PersistMapCleanup
import com.exilonium.compose.persist.persistMap
import com.exilonium.compose.routing.RouteHandler
import com.exilonium.innertube.Innertube
import com.exilonium.innertube.models.bodies.ContinuationBody
import com.exilonium.innertube.models.bodies.SearchBody
import com.exilonium.innertube.requests.searchPage
import com.exilonium.innertube.utils.from
import com.exilonium.voxify.LocalPlayerServiceBinder
import com.exilonium.voxify.R
import com.exilonium.voxify.preferences.UIStatePreferences
import com.exilonium.voxify.ui.components.LocalMenuState
import com.exilonium.voxify.ui.components.themed.Header
import com.exilonium.voxify.ui.components.themed.NonQueuedMediaItemMenu
import com.exilonium.voxify.ui.components.themed.Scaffold
import com.exilonium.voxify.ui.items.AlbumItem
import com.exilonium.voxify.ui.items.AlbumItemPlaceholder
import com.exilonium.voxify.ui.items.ArtistItem
import com.exilonium.voxify.ui.items.ArtistItemPlaceholder
import com.exilonium.voxify.ui.items.PlaylistItem
import com.exilonium.voxify.ui.items.PlaylistItemPlaceholder
import com.exilonium.voxify.ui.items.SongItem
import com.exilonium.voxify.ui.items.SongItemPlaceholder
import com.exilonium.voxify.ui.items.VideoItem
import com.exilonium.voxify.ui.items.VideoItemPlaceholder
import com.exilonium.voxify.ui.screens.GlobalRoutes
import com.exilonium.voxify.ui.screens.Route
import com.exilonium.voxify.ui.screens.albumRoute
import com.exilonium.voxify.ui.screens.artistRoute
import com.exilonium.voxify.ui.screens.playlistRoute
import com.exilonium.voxify.ui.styling.Dimensions
import com.exilonium.voxify.ui.styling.px
import com.exilonium.voxify.utils.asMediaItem
import com.exilonium.voxify.utils.forcePlay

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Route
@Composable
fun SearchResultScreen(query: String, onSearchAgain: () -> Unit) {
    val context = LocalContext.current
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(tagPrefix = "searchResults/$query/")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit = {
                Header(
                    title = query,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            context.persistMap?.keys?.removeAll {
                                it.startsWith("searchResults/$query/")
                            }
                            onSearchAgain()
                        }
                    }
                )
            }

            val emptyItemsText = stringResource(R.string.no_search_results)

            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = UIStatePreferences.searchResultScreenTabIndex,
                onTabChanged = { UIStatePreferences.searchResultScreenTabIndex = it },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                    item(1, stringResource(R.string.albums), R.drawable.disc)
                    item(2, stringResource(R.string.artists), R.drawable.person)
                    item(3, stringResource(R.string.videos), R.drawable.film)
                    item(4, stringResource(R.string.playlists), R.drawable.playlist)
                }
            ) { tabIndex ->
                saveableStateHolder.SaveableStateProvider(tabIndex) {
                    when (tabIndex) {
                        0 -> {
                            val binder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailSizeDp = Dimensions.thumbnails.song
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/songs",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) Innertube.searchPage(
                                        body = SearchBody(
                                            query = query,
                                            params = Innertube.SearchFilter.Song.value
                                        ),
                                        fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                    ) else Innertube.searchPage(
                                        body = ContinuationBody(continuation = continuation),
                                        fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                    )
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { song ->
                                    SongItem(
                                        song = song,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier.combinedClickable(
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        onDismiss = menuState::hide,
                                                        mediaItem = song.asMediaItem
                                                    )
                                                }
                                            },
                                            onClick = {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlay(song.asMediaItem)
                                                binder?.setupRadio(song.info?.endpoint)
                                            }
                                        )
                                    )
                                },
                                itemPlaceholderContent = {
                                    SongItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        1 -> {
                            val thumbnailSizeDp = Dimensions.thumbnails.album
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/albums",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Innertube.searchPage(
                                            body = SearchBody(
                                                query = query,
                                                params = Innertube.SearchFilter.Album.value
                                            ),
                                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                        )
                                    } else {
                                        Innertube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { album ->
                                    AlbumItem(
                                        album = album,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier.clickable(onClick = { albumRoute(album.key) })
                                    )
                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        2 -> {
                            val thumbnailSizeDp = 64.dp
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/artists",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Innertube.searchPage(
                                            body = SearchBody(
                                                query = query,
                                                params = Innertube.SearchFilter.Artist.value
                                            ),
                                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                        )
                                    } else {
                                        Innertube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { artist ->
                                    ArtistItem(
                                        artist = artist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier
                                            .clickable(onClick = { artistRoute(artist.key) })
                                    )
                                },
                                itemPlaceholderContent = {
                                    ArtistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }

                        3 -> {
                            val binder = LocalPlayerServiceBinder.current
                            val menuState = LocalMenuState.current
                            val thumbnailHeightDp = 72.dp
                            val thumbnailWidthDp = 128.dp

                            ItemsPage(
                                tag = "searchResults/$query/videos",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) {
                                        Innertube.searchPage(
                                            body = SearchBody(
                                                query = query,
                                                params = Innertube.SearchFilter.Video.value
                                            ),
                                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                                        )
                                    } else {
                                        Innertube.searchPage(
                                            body = ContinuationBody(continuation = continuation),
                                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                                        )
                                    }
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { video ->
                                    VideoItem(
                                        video = video,
                                        thumbnailWidthDp = thumbnailWidthDp,
                                        thumbnailHeightDp = thumbnailHeightDp,
                                        modifier = Modifier.combinedClickable(
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        mediaItem = video.asMediaItem,
                                                        onDismiss = menuState::hide
                                                    )
                                                }
                                            },
                                            onClick = {
                                                binder?.stopRadio()
                                                binder?.player?.forcePlay(video.asMediaItem)
                                                binder?.setupRadio(video.info?.endpoint)
                                            }
                                        )
                                    )
                                },
                                itemPlaceholderContent = {
                                    VideoItemPlaceholder(
                                        thumbnailHeightDp = thumbnailHeightDp,
                                        thumbnailWidthDp = thumbnailWidthDp
                                    )
                                }
                            )
                        }

                        4 -> {
                            val thumbnailSizeDp = Dimensions.thumbnails.playlist
                            val thumbnailSizePx = thumbnailSizeDp.px

                            ItemsPage(
                                tag = "searchResults/$query/playlists",
                                itemsPageProvider = { continuation ->
                                    if (continuation == null) Innertube.searchPage(
                                        body = SearchBody(
                                            query = query,
                                            params = Innertube.SearchFilter.CommunityPlaylist.value
                                        ),
                                        fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                    ) else Innertube.searchPage(
                                        body = ContinuationBody(continuation = continuation),
                                        fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                    )
                                },
                                emptyItemsText = emptyItemsText,
                                headerContent = headerContent,
                                itemContent = { playlist ->
                                    PlaylistItem(
                                        playlist = playlist,
                                        thumbnailSizePx = thumbnailSizePx,
                                        thumbnailSizeDp = thumbnailSizeDp,
                                        modifier = Modifier.clickable(onClick = {
                                            playlistRoute(playlist.key)
                                        })
                                    )
                                },
                                itemPlaceholderContent = {
                                    PlaylistItemPlaceholder(thumbnailSizeDp = thumbnailSizeDp)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
