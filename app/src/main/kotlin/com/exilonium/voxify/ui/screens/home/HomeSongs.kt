package com.exilonium.voxify.ui.screens.home

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.exilonium.compose.persist.persistList
import com.exilonium.voxify.Database
import com.exilonium.voxify.LocalPlayerAwareWindowInsets
import com.exilonium.voxify.LocalPlayerServiceBinder
import com.exilonium.voxify.R
import com.exilonium.voxify.enums.SongSortBy
import com.exilonium.voxify.enums.SortOrder
import com.exilonium.voxify.models.Song
import com.exilonium.voxify.preferences.AppearancePreferences
import com.exilonium.voxify.preferences.OrderPreferences
import com.exilonium.voxify.service.isLocal
import com.exilonium.voxify.transaction
import com.exilonium.voxify.ui.components.LocalMenuState
import com.exilonium.voxify.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.exilonium.voxify.ui.components.themed.Header
import com.exilonium.voxify.ui.components.themed.HeaderIconButton
import com.exilonium.voxify.ui.components.themed.InHistoryMediaItemMenu
import com.exilonium.voxify.ui.components.themed.TextField
import com.exilonium.voxify.ui.items.SongItem
import com.exilonium.voxify.ui.modifiers.swipeToClose
import com.exilonium.voxify.ui.screens.Route
import com.exilonium.voxify.ui.styling.Dimensions
import com.exilonium.voxify.ui.styling.LocalAppearance
import com.exilonium.voxify.ui.styling.onOverlay
import com.exilonium.voxify.ui.styling.overlay
import com.exilonium.voxify.ui.styling.px
import com.exilonium.voxify.utils.asMediaItem
import com.exilonium.voxify.utils.center
import com.exilonium.voxify.utils.color
import com.exilonium.voxify.utils.forcePlayAtIndex
import com.exilonium.voxify.utils.secondary
import com.exilonium.voxify.utils.semiBold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Song.formattedTotalPlayTime: String
    @Composable get() {
        val seconds = totalPlayTimeMs / 1000

        val hours = seconds / 3600

        return when {
            hours == 0L -> stringResource(id = R.string.format_minutes, seconds / 60)
            hours < 24L -> stringResource(id = R.string.format_hours, hours)
            else -> stringResource(id = R.string.format_days, hours / 24)
        }
    }

@Composable
fun HomeSongs(
    onSearchClick: () -> Unit
) = with(OrderPreferences) {
    HomeSongs(
        onSearchClick = onSearchClick,
        songProvider = {
            Database.songs(songSortBy, songSortOrder)
                .map { songs -> songs.filter { it.totalPlayTimeMs > 0L } }
        },
        sortBy = songSortBy,
        setSortBy = { songSortBy = it },
        sortOrder = songSortOrder,
        setSortOrder = { songSortOrder = it },
        title = stringResource(R.string.songs)
    )
}

@kotlin.OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)
@OptIn(UnstableApi::class)
@Route
@Composable
fun HomeSongs(
    onSearchClick: () -> Unit,
    songProvider: () -> Flow<List<Song>>,
    sortBy: SongSortBy,
    setSortBy: (SongSortBy) -> Unit,
    sortOrder: SortOrder,
    setSortOrder: (SortOrder) -> Unit,
    title: String
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val typography = LocalAppearance.current.typography
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    var filter: String? by rememberSaveable { mutableStateOf(null) }
    var items by persistList<Song>("home/songs")
    val filteredItems by remember {
        derivedStateOf {
            filter?.lowercase()?.ifBlank { null }?.let { f ->
                items.filter {
                    f in it.title.lowercase() || f in it.artistsText?.lowercase().orEmpty()
                }.sortedBy { it.title }
            } ?: items
        }
    }

    LaunchedEffect(sortBy, sortOrder) {
        songProvider().collect { items = it }
    }

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = ""
    )

    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier
            .background(colorPalette.background0)
            .fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                Header(title = title) {
                    var searching by rememberSaveable { mutableStateOf(false) }

                    AnimatedContent(
                        targetState = searching,
                        label = ""
                    ) { state ->
                        if (state) {
                            val focusRequester = remember { FocusRequester() }

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }

                            TextField(
                                value = filter.orEmpty(),
                                onValueChange = { filter = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (filter.isNullOrBlank()) filter = ""
                                    focusManager.clearFocus()
                                }),
                                hintText = stringResource(R.string.filter_placeholder),
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        if (!it.hasFocus) {
                                            keyboardController?.hide()
                                            if (filter?.isBlank() == true) {
                                                filter = null
                                                searching = false
                                            }
                                        }
                                    }
                            )
                        } else Row(verticalAlignment = Alignment.CenterVertically) {
                            HeaderIconButton(
                                onClick = { searching = true },
                                icon = R.drawable.search,
                                color = colorPalette.text
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            if (items.isNotEmpty()) BasicText(
                                text = pluralStringResource(
                                    R.plurals.song_count_plural,
                                    items.size,
                                    items.size
                                ),
                                style = typography.xs.secondary.semiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    HeaderIconButton(
                        icon = R.drawable.trending,
                        color = if (sortBy == SongSortBy.PlayTime) colorPalette.text else colorPalette.textDisabled,
                        onClick = { setSortBy(SongSortBy.PlayTime) }
                    )

                    HeaderIconButton(
                        icon = R.drawable.text,
                        color = if (sortBy == SongSortBy.Title) colorPalette.text else colorPalette.textDisabled,
                        onClick = { setSortBy(SongSortBy.Title) }
                    )

                    HeaderIconButton(
                        icon = R.drawable.time,
                        color = if (sortBy == SongSortBy.DateAdded) colorPalette.text else colorPalette.textDisabled,
                        onClick = { setSortBy(SongSortBy.DateAdded) }
                    )

                    HeaderIconButton(
                        icon = R.drawable.arrow_up,
                        color = colorPalette.text,
                        onClick = { setSortOrder(!sortOrder) },
                        modifier = Modifier.graphicsLayer { rotationZ = sortOrderIconRotation }
                    )
                }
            }

            items(
                items = filteredItems,
                key = { song -> song.id }
            ) { song ->
                SongItem(
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                keyboardController?.hide()
                                menuState.display {
                                    InHistoryMediaItemMenu(
                                        song = song,
                                        onDismiss = menuState::hide
                                    )
                                }
                            },
                            onClick = {
                                keyboardController?.hide()
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(
                                    items.map(Song::asMediaItem),
                                    items.indexOf(song)
                                )
                            }
                        )
                        .animateItemPlacement()
                        .let {
                            if (!song.isLocal && AppearancePreferences.swipeToHideSong) it.swipeToClose {
                                binder?.cache?.removeResource(song.id)
                                transaction {
                                    Database.delete(song)
                                }
                            } else it
                        },
                    song = song,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    onThumbnailContent = if (sortBy == SongSortBy.PlayTime) {
                        {
                            BasicText(
                                text = song.formattedTotalPlayTime,
                                style = typography.xxs.semiBold.center.color(colorPalette.onOverlay),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, colorPalette.overlay)
                                        ),
                                        shape = thumbnailShape.copy(
                                            topStart = CornerSize(0.dp),
                                            topEnd = CornerSize(0.dp)
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    } else null
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )
    }
}
