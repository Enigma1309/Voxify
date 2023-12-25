package com.exilonium.voxify.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.exilonium.compose.persist.persist
import com.exilonium.innertube.Innertube
import com.exilonium.innertube.requests.discoverPage
import com.exilonium.voxify.LocalPlayerAwareWindowInsets
import com.exilonium.voxify.R
import com.exilonium.voxify.ui.components.ShimmerHost
import com.exilonium.voxify.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.exilonium.voxify.ui.components.themed.Header
import com.exilonium.voxify.ui.components.themed.TextPlaceholder
import com.exilonium.voxify.ui.items.AlbumItem
import com.exilonium.voxify.ui.items.AlbumItemPlaceholder
import com.exilonium.voxify.ui.screens.Route
import com.exilonium.voxify.ui.styling.Dimensions
import com.exilonium.voxify.ui.styling.LocalAppearance
import com.exilonium.voxify.ui.styling.px
import com.exilonium.voxify.ui.styling.shimmer
import com.exilonium.voxify.utils.center
import com.exilonium.voxify.utils.isLandscape
import com.exilonium.voxify.utils.rememberSnapLayoutInfoProvider
import com.exilonium.voxify.utils.secondary
import com.exilonium.voxify.utils.semiBold

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Route
@Composable
fun HomeDiscovery(
    onMoodClick: (mood: Innertube.Mood.Item) -> Unit,
    onNewReleaseAlbumClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val windowInsets = LocalPlayerAwareWindowInsets.current

    val scrollState = rememberScrollState()
    val lazyGridState = rememberLazyGridState()

    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    val thumbnailDp = Dimensions.thumbnails.album
    val thumbnailPx = thumbnailDp.px

    var discoverPage by persist<Result<Innertube.DiscoverPage>>("home/discovery")

    LaunchedEffect(Unit) {
        discoverPage = Innertube.discoverPage()
    }

    BoxWithConstraints {
        val moodItemWidthFactor = if (isLandscape && maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f

        val snapLayoutInfoProvider = rememberSnapLayoutInfoProvider(
            lazyGridState = lazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                layoutSize * moodItemWidthFactor / 2f - itemSize / 2f
            }
        )

        val itemWidth = maxWidth * moodItemWidthFactor

        Column(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Vertical)
                        .asPaddingValues()
                )
        ) {
            Header(
                title = stringResource(R.string.discover),
                modifier = Modifier.padding(endPaddingValues)
            )

            discoverPage?.getOrNull()?.let { page ->
                if (page.moods.isNotEmpty()) {
                    BasicText(
                        text = stringResource(R.string.moods_and_genres),
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyHorizontalGrid(
                        state = lazyGridState,
                        rows = GridCells.Fixed(4),
                        flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                        contentPadding = endPaddingValues,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((4 * (64 + 4)).dp)
                    ) {
                        items(
                            items = page.moods.sortedBy { it.title },
                            key = { it.endpoint.params ?: it.title }
                        ) {
                            MoodItem(
                                mood = it,
                                onClick = { it.endpoint.browseId?.let { _ -> onMoodClick(it) } },
                                modifier = Modifier
                                    .width(itemWidth)
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                if (page.newReleaseAlbums.isNotEmpty()) {
                    BasicText(
                        text = stringResource(R.string.new_released_albums),
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(items = page.newReleaseAlbums, key = { it.key }) {
                            AlbumItem(
                                album = it,
                                thumbnailSizePx = thumbnailPx,
                                thumbnailSizeDp = thumbnailDp,
                                alternative = true,
                                modifier = Modifier.clickable(onClick = { onNewReleaseAlbumClick(it.key) })
                            )
                        }
                    }
                }
            } ?: discoverPage?.exceptionOrNull()?.let {
                BasicText(
                    text = stringResource(R.string.error_message),
                    style = typography.s.secondary.center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(all = 16.dp)
                )
            } ?: ShimmerHost {
                TextPlaceholder(modifier = sectionTextModifier)
                LazyHorizontalGrid(
                    state = lazyGridState,
                    rows = GridCells.Fixed(4),
                    flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
                    contentPadding = endPaddingValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((4 * (64 + 4)).dp)
                ) {
                    items(16) {
                        MoodItemPlaceholder(
                            width = itemWidth,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                TextPlaceholder(modifier = sectionTextModifier)
                Row {
                    repeat(2) {
                        AlbumItemPlaceholder(
                            thumbnailSizeDp = thumbnailDp,
                            alternative = true
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            scrollState = scrollState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )
    }
}

@Composable
fun MoodItem(
    mood: Innertube.Mood.Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = LocalAppearance.current.typography
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    val moodColor by remember { derivedStateOf { Color(mood.stripeColor) } }
    val textColor by remember {
        derivedStateOf {
            if (moodColor.luminance() >= 0.5f) Color.Black else Color.White
        }
    }

    ElevatedCard(
        modifier = modifier.height(64.dp),
        shape = thumbnailShape,
        colors = CardDefaults.elevatedCardColors(containerColor = moodColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
            contentAlignment = Alignment.CenterStart
        ) {
            BasicText(
                text = mood.title,
                style = typography.xs.semiBold.copy(color = textColor),
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

@Composable
fun MoodItemPlaceholder(
    width: Dp,
    modifier: Modifier = Modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    Spacer(
        modifier = modifier
            .background(color = colorPalette.shimmer, shape = thumbnailShape)
            .size(width, 64.dp)
    )
}
