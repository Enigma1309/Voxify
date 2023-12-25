package com.exilonium.voxify.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.exilonium.innertube.Innertube
import com.exilonium.voxify.ui.components.themed.TextPlaceholder
import com.exilonium.voxify.ui.styling.LocalAppearance
import com.exilonium.voxify.ui.styling.onOverlay
import com.exilonium.voxify.ui.styling.overlay
import com.exilonium.voxify.ui.styling.shimmer
import com.exilonium.voxify.utils.color
import com.exilonium.voxify.utils.medium
import com.exilonium.voxify.utils.secondary
import com.exilonium.voxify.utils.semiBold

@Composable
fun VideoItem(
    video: Innertube.VideoItem,
    thumbnailHeightDp: Dp,
    thumbnailWidthDp: Dp,
    modifier: Modifier = Modifier
) = VideoItem(
    thumbnailUrl = video.thumbnail?.url,
    duration = video.durationText,
    title = video.info?.name,
    uploader = video.authors?.joinToString("") { it.name.orEmpty() },
    views = video.viewsText,
    thumbnailHeightDp = thumbnailHeightDp,
    thumbnailWidthDp = thumbnailWidthDp,
    modifier = modifier
)

@Composable
fun VideoItem(
    thumbnailUrl: String?,
    duration: String?,
    title: String?,
    uploader: String?,
    views: String?,
    thumbnailHeightDp: Dp,
    thumbnailWidthDp: Dp,
    modifier: Modifier = Modifier
) = ItemContainer(
    alternative = false,
    thumbnailSizeDp = 0.dp,
    modifier = modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val typography = LocalAppearance.current.typography
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    Box {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .clip(thumbnailShape)
                .size(width = thumbnailWidthDp, height = thumbnailHeightDp)
        )

        duration?.let {
            BasicText(
                text = duration,
                style = typography.xxs.medium.color(colorPalette.onOverlay),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(all = 4.dp)
                    .background(color = colorPalette.overlay, shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }

    ItemInfoContainer {
        BasicText(
            text = title.orEmpty(),
            style = typography.xs.semiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        BasicText(
            text = uploader.orEmpty(),
            style = typography.xs.semiBold.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        views?.let {
            BasicText(
                text = views,
                style = typography.xxs.medium.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun VideoItemPlaceholder(
    thumbnailHeightDp: Dp,
    thumbnailWidthDp: Dp,
    modifier: Modifier = Modifier
) = ItemContainer(
    alternative = false,
    thumbnailSizeDp = 0.dp,
    modifier = modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    Spacer(
        modifier = Modifier
            .background(color = colorPalette.shimmer, shape = thumbnailShape)
            .size(width = thumbnailWidthDp, height = thumbnailHeightDp)
    )

    ItemInfoContainer {
        TextPlaceholder()
        TextPlaceholder()
        TextPlaceholder(modifier = Modifier.padding(top = 8.dp))
    }
}
