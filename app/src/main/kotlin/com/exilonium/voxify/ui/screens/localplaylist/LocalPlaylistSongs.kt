package com.exilonium.voxify.ui.screens.localplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exilonium.compose.persist.persist
import com.exilonium.compose.reordering.draggedItem
import com.exilonium.compose.reordering.rememberReorderingState
import com.exilonium.compose.reordering.reorder
import com.exilonium.innertube.Innertube
import com.exilonium.innertube.models.bodies.BrowseBody
import com.exilonium.innertube.requests.playlistPage
import com.exilonium.voxify.Database
import com.exilonium.voxify.LocalPlayerAwareWindowInsets
import com.exilonium.voxify.LocalPlayerServiceBinder
import com.exilonium.voxify.R
import com.exilonium.voxify.models.PlaylistWithSongs
import com.exilonium.voxify.models.Song
import com.exilonium.voxify.models.SongPlaylistMap
import com.exilonium.voxify.query
import com.exilonium.voxify.transaction
import com.exilonium.voxify.ui.components.LocalMenuState
import com.exilonium.voxify.ui.components.themed.ConfirmationDialog
import com.exilonium.voxify.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.exilonium.voxify.ui.components.themed.Header
import com.exilonium.voxify.ui.components.themed.HeaderIconButton
import com.exilonium.voxify.ui.components.themed.IconButton
import com.exilonium.voxify.ui.components.themed.InPlaylistMediaItemMenu
import com.exilonium.voxify.ui.components.themed.Menu
import com.exilonium.voxify.ui.components.themed.MenuEntry
import com.exilonium.voxify.ui.components.themed.SecondaryTextButton
import com.exilonium.voxify.ui.components.themed.TextFieldDialog
import com.exilonium.voxify.ui.items.SongItem
import com.exilonium.voxify.ui.styling.Dimensions
import com.exilonium.voxify.ui.styling.LocalAppearance
import com.exilonium.voxify.ui.styling.px
import com.exilonium.voxify.utils.PlaylistDownloadIcon
import com.exilonium.voxify.utils.asMediaItem
import com.exilonium.voxify.utils.completed
import com.exilonium.voxify.utils.enqueue
import com.exilonium.voxify.utils.forcePlayAtIndex
import com.exilonium.voxify.utils.forcePlayFromBeginning
import com.exilonium.voxify.utils.launchYouTubeMusic
import com.exilonium.voxify.utils.toast
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LocalPlaylistSongs(
    playlistId: Long,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var playlistWithSongs by persist<PlaylistWithSongs?>("localPlaylist/$playlistId/playlistWithSongs")

    LaunchedEffect(Unit) {
        Database.playlistWithSongs(playlistId).filterNotNull().collect { playlistWithSongs = it }
    }

    val lazyListState = rememberLazyListState()

    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = playlistWithSongs?.songs ?: emptyList<Any>(),
        onDragEnd = { fromIndex, toIndex ->
            query {
                Database.move(playlistId, fromIndex, toIndex)
            }
        },
        extraItemCount = 1
    )

    var isRenaming by rememberSaveable { mutableStateOf(false) }

    if (isRenaming) TextFieldDialog(
        hintText = stringResource(R.string.enter_playlist_name_prompt),
        initialTextInput = playlistWithSongs?.playlist?.name.orEmpty(),
        onDismiss = { isRenaming = false },
        onDone = { text ->
            query {
                playlistWithSongs?.playlist?.copy(name = text)?.let(Database::update)
            }
        }
    )

    var isDeleting by rememberSaveable { mutableStateOf(false) }

    if (isDeleting) ConfirmationDialog(
        text = stringResource(R.string.confirm_delete_playlist),
        onDismiss = { isDeleting = false },
        onConfirm = {
            query {
                playlistWithSongs?.playlist?.let(Database::delete)
            }
            onDelete()
        }
    )

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    Box(modifier = modifier) {
        LazyColumn(
            state = reorderingState.lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                Header(
                    title = playlistWithSongs?.playlist?.name ?: stringResource(R.string.unknown),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.enqueue),
                        enabled = playlistWithSongs?.songs?.isNotEmpty() == true,
                        onClick = {
                            playlistWithSongs?.songs?.map(Song::asMediaItem)?.let { mediaItems ->
                                binder?.player?.enqueue(mediaItems)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    playlistWithSongs?.songs?.map(Song::asMediaItem)
                        ?.let { PlaylistDownloadIcon(songs = it.toImmutableList()) }

                    HeaderIconButton(
                        icon = R.drawable.ellipsis_horizontal,
                        color = colorPalette.text,
                        onClick = {
                            menuState.display {
                                Menu {
                                    playlistWithSongs?.playlist?.browseId?.let { browseId ->
                                        MenuEntry(
                                            icon = R.drawable.sync,
                                            text = stringResource(R.string.sync),
                                            onClick = {
                                                menuState.hide()
                                                transaction {
                                                    runBlocking(Dispatchers.IO) {
                                                        Innertube.playlistPage(BrowseBody(browseId = browseId))
                                                            ?.completed()
                                                    }?.getOrNull()?.let { remotePlaylist ->
                                                        Database.clearPlaylist(playlistId)

                                                        remotePlaylist.songsPage
                                                            ?.items
                                                            ?.map(Innertube.SongItem::asMediaItem)
                                                            ?.onEach(Database::insert)
                                                            ?.mapIndexed { position, mediaItem ->
                                                                SongPlaylistMap(
                                                                    songId = mediaItem.mediaId,
                                                                    playlistId = playlistId,
                                                                    position = position
                                                                )
                                                            }?.let(Database::insertSongPlaylistMaps)
                                                    }
                                                }
                                            }
                                        )

                                        playlistWithSongs?.songs?.firstOrNull()?.id?.let { firstSongId ->
                                            MenuEntry(
                                                icon = R.drawable.play,
                                                text = stringResource(R.string.watch_playlist_on_youtube),
                                                onClick = {
                                                    menuState.hide()
                                                    binder?.player?.pause()
                                                    uriHandler.openUri(
                                                        "https://youtube.com/watch?v=$firstSongId&list=${
                                                            playlistWithSongs?.playlist?.browseId
                                                                ?.drop(2)
                                                        }"
                                                    )
                                                }
                                            )

                                            MenuEntry(
                                                icon = R.drawable.musical_notes,
                                                text = stringResource(R.string.open_in_youtube_music),
                                                onClick = {
                                                    menuState.hide()
                                                    binder?.player?.pause()
                                                    if (
                                                        !launchYouTubeMusic(
                                                            context = context,
                                                            endpoint = "watch?v=$firstSongId&list=${
                                                                playlistWithSongs?.playlist?.browseId
                                                                    ?.drop(2)
                                                            }"
                                                        )
                                                    ) context.toast(
                                                        context.getString(R.string.youtube_music_not_installed)
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    MenuEntry(
                                        icon = R.drawable.pencil,
                                        text = stringResource(R.string.rename),
                                        onClick = {
                                            menuState.hide()
                                            isRenaming = true
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.trash,
                                        text = stringResource(R.string.delete),
                                        onClick = {
                                            menuState.hide()
                                            isDeleting = true
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }

            itemsIndexed(
                items = playlistWithSongs?.songs ?: emptyList(),
                key = { _, song -> song.id },
                contentType = { _, song -> song }
            ) { index, song ->
                SongItem(
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                menuState.display {
                                    InPlaylistMediaItemMenu(
                                        playlistId = playlistId,
                                        positionInPlaylist = index,
                                        song = song,
                                        onDismiss = menuState::hide
                                    )
                                }
                            },
                            onClick = {
                                playlistWithSongs?.songs
                                    ?.map(Song::asMediaItem)
                                    ?.let { mediaItems ->
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(mediaItems, index)
                                    }
                            }
                        )
                        .draggedItem(reorderingState = reorderingState, index = index)
                        .background(colorPalette.background1),
                    song = song,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp
                ) {
                    IconButton(
                        icon = R.drawable.reorder,
                        color = colorPalette.textDisabled,
                        indication = null,
                        onClick = {},
                        modifier = Modifier
                            .reorder(reorderingState = reorderingState, index = index)
                            .size(18.dp)
                    )
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.shuffle,
            visible = !reorderingState.isDragging,
            onClick = {
                playlistWithSongs?.songs?.let { songs ->
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(
                            songs.shuffled().map(Song::asMediaItem)
                        )
                    }
                }
            }
        )
    }
}
