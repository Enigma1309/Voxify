package com.exilonium.voxify.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.exilonium.innertube.Innertube
import com.exilonium.voxify.models.Album
import com.exilonium.voxify.ui.components.themed.TextPlaceholder
import com.exilonium.voxify.ui.styling.LocalAppearance
import com.exilonium.voxify.ui.styling.shimmer
import com.exilonium.voxify.utils.secondary
import com.exilonium.voxify.utils.semiBold
import com.exilonium.voxify.utils.thumbnail

@Composable
fun AlbumItem(
    album: Album,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = AlbumItem(
    thumbnailUrl = album.thumbnailUrl,
    title = album.title,
    authors = album.authorsText,
    year = album.year,
    thumbnailSizePx = thumbnailSizePx,
    thumbnailSizeDp = thumbnailSizeDp,
    alternative = alternative,
    modifier = modifier
)

@Composable
fun AlbumItem(
    album: Innertube.AlbumItem,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = AlbumItem(
    thumbnailUrl = album.thumbnail?.url,
    title = album.info?.name,
    authors = album.authors?.joinToString("") { it.name.orEmpty() },
    year = album.year,
    thumbnailSizePx = thumbnailSizePx,
    thumbnailSizeDp = thumbnailSizeDp,
    alternative = alternative,
    modifier = modifier
)

@Composable
fun AlbumItem(
    thumbnailUrl: String?,
    title: String?,
    authors: String?,
    year: String?,
    thumbnailSizePx: Int,
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ItemContainer(
    alternative = alternative,
    thumbnailSizeDp = thumbnailSizeDp,
    modifier = modifier
) {
    val typography = LocalAppearance.current.typography
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    AsyncImage(
        model = thumbnailUrl?.thumbnail(thumbnailSizePx),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .clip(thumbnailShape)
            .size(thumbnailSizeDp)
    )

    ItemInfoContainer {
        BasicText(
            text = title.orEmpty(),
            style = typography.xs.semiBold,
            maxLines = if (alternative) 1 else 2,
            overflow = TextOverflow.Ellipsis
        )

        if (!alternative) authors?.let {
            BasicText(
                text = authors,
                style = typography.xs.semiBold.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        BasicText(
            text = year.orEmpty(),
            style = typography.xxs.semiBold.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun AlbumItemPlaceholder(
    thumbnailSizeDp: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ItemContainer(
    alternative = alternative,
    thumbnailSizeDp = thumbnailSizeDp,
    modifier = modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    Spacer(
        modifier = Modifier
            .background(color = colorPalette.shimmer, shape = thumbnailShape)
            .size(thumbnailSizeDp)
    )

    ItemInfoContainer {
        TextPlaceholder()
        if (!alternative) TextPlaceholder()
        TextPlaceholder(modifier = Modifier.padding(top = 4.dp))
    }
}
