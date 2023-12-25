package com.exilonium.voxify.ui.screens.builtinplaylist

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.exilonium.compose.persist.persistList
import com.exilonium.voxify.Database
import com.exilonium.voxify.LocalPlayerAwareWindowInsets
import com.exilonium.voxify.LocalPlayerServiceBinder
import com.exilonium.voxify.R
import com.exilonium.voxify.enums.BuiltInPlaylist
import com.exilonium.voxify.models.Song
import com.exilonium.voxify.preferences.DataPreferences
import com.exilonium.voxify.ui.components.LocalMenuState
import com.exilonium.voxify.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.exilonium.voxify.ui.components.themed.Header
import com.exilonium.voxify.ui.components.themed.InHistoryMediaItemMenu
import com.exilonium.voxify.ui.components.themed.NonQueuedMediaItemMenu
import com.exilonium.voxify.ui.components.themed.SecondaryTextButton
import com.exilonium.voxify.ui.components.themed.ValueSelectorDialog
import com.exilonium.voxify.ui.items.SongItem
import com.exilonium.voxify.ui.styling.Dimensions
import com.exilonium.voxify.ui.styling.LocalAppearance
import com.exilonium.voxify.ui.styling.px
import com.exilonium.voxify.utils.asMediaItem
import com.exilonium.voxify.utils.enqueue
import com.exilonium.voxify.utils.forcePlayAtIndex
import com.exilonium.voxify.utils.forcePlayFromBeginning
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlin.math.min

@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@OptIn(UnstableApi::class)
@Composable
fun BuiltInPlaylistSongs(
    builtInPlaylist: BuiltInPlaylist,
    modifier: Modifier = Modifier
) = with(DataPreferences) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    var songs by persistList<Song>("${builtInPlaylist.name}/songs")

    LaunchedEffect(binder) {
        when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> Database.favorites()

            BuiltInPlaylist.Offline -> Database.songsWithContentLength().map { songs ->
                songs.filter { binder?.isCached(it) ?: false }.map { it.song }
            }

            BuiltInPlaylist.Top -> snapshotFlow { topListPeriod to topListLength }
                .distinctUntilChanged().transformLatest { (period, length) ->
                    emitAll(
                        if (period.duration != null) Database.trending(
                            limit = length,
                            period = period.duration.inWholeMilliseconds
                        ) else Database.songsByPlayTimeDesc().distinctUntilChanged()
                            .map { it.subList(0, min(length, it.size)) }.cancellable()
                    )
                }
        }.collect { songs = it }
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSize = thumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    Box(modifier = modifier) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(key = "header", contentType = 0) {
                Header(
                    title = when (builtInPlaylist) {
                        BuiltInPlaylist.Favorites -> stringResource(R.string.favorites)
                        BuiltInPlaylist.Offline -> stringResource(R.string.offline)
                        BuiltInPlaylist.Top -> stringResource(
                            R.string.format_my_top_playlist,
                            topListLength
                        )
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.enqueue),
                        enabled = songs.isNotEmpty(),
                        onClick = {
                            binder?.player?.enqueue(songs.map(Song::asMediaItem))
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (builtInPlaylist == BuiltInPlaylist.Top) {
                        var dialogShowing by rememberSaveable { mutableStateOf(false) }

                        SecondaryTextButton(
                            text = topListPeriod.displayName(),
                            onClick = { dialogShowing = true }
                        )

                        if (dialogShowing) ValueSelectorDialog(
                            onDismiss = { dialogShowing = false },
                            title = stringResource(R.string.format_view_top_of_header, topListLength),
                            selectedValue = topListPeriod,
                            values = DataPreferences.TopListPeriod.entries.toImmutableList(),
                            onValueSelected = { topListPeriod = it },
                            valueText = { it.displayName() }
                        )
                    }
                }
            }

            itemsIndexed(
                items = songs,
                key = { _, song -> song.id },
                contentType = { _, song -> song }
            ) { index, song ->
                Row {
                    SongItem(
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    menuState.display {
                                        when (builtInPlaylist) {
                                            BuiltInPlaylist.Favorites -> NonQueuedMediaItemMenu(
                                                mediaItem = song.asMediaItem,
                                                onDismiss = menuState::hide
                                            )

                                            BuiltInPlaylist.Offline -> InHistoryMediaItemMenu(
                                                song = song,
                                                onDismiss = menuState::hide
                                            )

                                            BuiltInPlaylist.Top -> NonQueuedMediaItemMenu(
                                                mediaItem = song.asMediaItem,
                                                onDismiss = menuState::hide
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.player?.forcePlayAtIndex(
                                        items = songs.map(Song::asMediaItem),
                                        index = index
                                    )
                                }
                            )
                            .animateItemPlacement(),
                        song = song,
                        index = if (builtInPlaylist == BuiltInPlaylist.Top) index else null,
                        thumbnailSizePx = thumbnailSize,
                        thumbnailSizeDp = thumbnailSizeDp
                    )
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.shuffle,
            onClick = {
                if (songs.isEmpty()) return@FloatingActionsContainerWithScrollToTop
                binder?.stopRadio()
                binder?.player?.forcePlayFromBeginning(
                    songs.shuffled().map(Song::asMediaItem)
                )
            }
        )
    }
}
