package com.exilonium.voxify.ui.screens.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.exilonium.compose.routing.OnGlobalRoute
import com.exilonium.innertube.models.NavigationEndpoint
import com.exilonium.voxify.Database
import com.exilonium.voxify.LocalPlayerServiceBinder
import com.exilonium.voxify.R
import com.exilonium.voxify.enums.ThumbnailRoundness
import com.exilonium.voxify.models.ui.toUiMedia
import com.exilonium.voxify.preferences.PlayerPreferences
import com.exilonium.voxify.query
import com.exilonium.voxify.roundedShape
import com.exilonium.voxify.service.PlayerService
import com.exilonium.voxify.ui.components.BottomSheet
import com.exilonium.voxify.ui.components.BottomSheetState
import com.exilonium.voxify.ui.components.LocalMenuState
import com.exilonium.voxify.ui.components.rememberBottomSheetState
import com.exilonium.voxify.ui.components.themed.BaseMediaItemMenu
import com.exilonium.voxify.ui.components.themed.IconButton
import com.exilonium.voxify.ui.components.themed.SecondaryTextButton
import com.exilonium.voxify.ui.components.themed.SliderDialog
import com.exilonium.voxify.ui.components.themed.TextToggle
import com.exilonium.voxify.ui.modifiers.onSwipe
import com.exilonium.voxify.ui.styling.Dimensions
import com.exilonium.voxify.ui.styling.LocalAppearance
import com.exilonium.voxify.ui.styling.collapsedPlayerProgressBar
import com.exilonium.voxify.ui.styling.px
import com.exilonium.voxify.utils.DisposableListener
import com.exilonium.voxify.utils.forceSeekToNext
import com.exilonium.voxify.utils.forceSeekToPrevious
import com.exilonium.voxify.utils.isLandscape
import com.exilonium.voxify.utils.positionAndDurationState
import com.exilonium.voxify.utils.seamlessPlay
import com.exilonium.voxify.utils.secondary
import com.exilonium.voxify.utils.semiBold
import com.exilonium.voxify.utils.shouldBePlaying
import com.exilonium.voxify.utils.thumbnail
import com.exilonium.voxify.utils.toast
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private fun onDismiss(binder: PlayerService.Binder) {
    binder.stopRadio()
    binder.player.clearMediaItems()
}

@kotlin.OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun Player(
    layoutState: BottomSheetState,
    modifier: Modifier = Modifier
) {
    val menuState = LocalMenuState.current

    val (colorPalette, typography, thumbnailCornerSize) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    binder?.player ?: return

    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, neverEqualPolicy())
    }

    var shouldBePlaying by remember {
        mutableStateOf(binder.player.shouldBePlaying)
    }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val mediaItem = nullableMediaItem ?: return

    val positionAndDuration by binder.player.positionAndDurationState()

    val windowInsets = WindowInsets.systemBars

    val horizontalBottomPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()

    OnGlobalRoute {
        layoutState.collapseSoft()
    }

    BottomSheet(
        state = layoutState,
        modifier = modifier,
        onDismiss = { onDismiss(binder) },
        collapsedContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .background(colorPalette.background1)
                    .fillMaxSize()
                    .padding(horizontalBottomPaddingValues)
                    .drawBehind {
                        val progress =
                            positionAndDuration.first.toFloat() / positionAndDuration.second.absoluteValue

                        drawLine(
                            color = colorPalette.collapsedPlayerProgressBar,
                            start = Offset(x = 0f, y = 1.dp.toPx()),
                            end = Offset(x = size.width * progress, y = 1.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    .let {
                        if (PlayerPreferences.horizontalSwipeToClose) it.onSwipe(
                            animateOffset = true,
                            onSwipeOut = {
                                layoutState.dismiss()
                                onDismiss(binder)
                            }
                        ) else it
                    }

            ) {
                Spacer(modifier = Modifier.width(2.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(Dimensions.collapsedPlayer)
                ) {
                    AsyncImage(
                        model = mediaItem.mediaMetadata.artworkUri.thumbnail(Dimensions.thumbnails.song.px),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(thumbnailCornerSize.coerceAtMost(ThumbnailRoundness.Heavy.dp).roundedShape)
                            .background(colorPalette.background0)
                            .size(48.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(Dimensions.collapsedPlayer)
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                        label = "",
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { text ->
                        BasicText(
                            text = text,
                            style = typography.xs.semiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AnimatedVisibility(visible = mediaItem.mediaMetadata.artist != null) {
                        AnimatedContent(
                            targetState = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                            label = "",
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { text ->
                            BasicText(
                                text = text,
                                style = typography.xs.semiBold.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(Dimensions.collapsedPlayer)
                ) {
                    AnimatedVisibility(visible = PlayerPreferences.isShowingPrevButtonCollapsed) {
                        IconButton(
                            icon = R.drawable.play_skip_back,
                            color = colorPalette.text,
                            onClick = binder.player::forceSeekToPrevious,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .size(20.dp)
                        )
                    }

                    IconButton(
                        icon = if (shouldBePlaying) R.drawable.pause else R.drawable.play,
                        color = colorPalette.text,
                        onClick = {
                            if (shouldBePlaying) binder.player.pause() else {
                                if (binder.player.playbackState == Player.STATE_IDLE) binder.player.prepare()
                                binder.player.play()
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .size(20.dp)
                    )

                    IconButton(
                        icon = R.drawable.play_skip_forward,
                        color = colorPalette.text,
                        onClick = binder.player::forceSeekToNext,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    ) {
        var isShowingStatsForNerds by rememberSaveable { mutableStateOf(false) }

        val playerBottomSheetState = rememberBottomSheetState(
            dismissedBound = 64.dp + horizontalBottomPaddingValues.calculateBottomPadding(),
            expandedBound = layoutState.expandedBound
        )

        val containerModifier = Modifier
            .background(colorPalette.background1)
            .padding(
                windowInsets
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    .asPaddingValues()
            )
            .padding(bottom = playerBottomSheetState.collapsedBound)

        val thumbnailContent: @Composable (modifier: Modifier) -> Unit = { innerModifier ->
            Thumbnail(
                isShowingLyrics = PlayerPreferences.isShowingLyrics,
                onShowLyrics = { PlayerPreferences.isShowingLyrics = it },
                isShowingStatsForNerds = isShowingStatsForNerds,
                onShowStatsForNerds = { isShowingStatsForNerds = it },
                modifier = innerModifier.nestedScroll(layoutState.preUpPostDownNestedScrollConnection)
            )
        }

        val controlsContent: @Composable (modifier: Modifier) -> Unit = { innerModifier ->
            val media = mediaItem.toUiMedia(positionAndDuration.second)

            Controls(
                media = media,
                shouldBePlaying = shouldBePlaying,
                position = positionAndDuration.first,
                modifier = innerModifier
            )
        }

        if (isLandscape) Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = containerModifier.padding(top = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(0.66f)
                    .padding(bottom = 16.dp)
            ) {
                thumbnailContent(Modifier.padding(horizontal = 16.dp))
            }

            controlsContent(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxHeight()
                    .weight(1f)
            )
        } else Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = containerModifier.padding(top = 54.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1.25f)
            ) {
                thumbnailContent(Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
            }

            controlsContent(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        var speedDialogOpen by rememberSaveable { mutableStateOf(false) }

        if (speedDialogOpen) SliderDialog(
            onDismiss = { speedDialogOpen = false },
            title = stringResource(R.string.playback_speed),
            initialValue = PlayerPreferences.speed * 100f,
            onSlide = { },
            onSlideCompleted = { PlayerPreferences.speed = it.roundToInt() / 100f },
            min = 0f,
            max = 200f,
            toDisplay = {
                if (it <= 1f) stringResource(R.string.minimum_speed_value)
                else stringResource(
                    R.string.format_speed_multiplier,
                    "%.2f".format(it.roundToInt() / 100f)
                )
            }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SecondaryTextButton(
                    text = stringResource(R.string.reset),
                    onClick = {
                        PlayerPreferences.speed = 1f
                        speedDialogOpen = false
                    }
                )
            }
        }

        var boostDialogOpen by rememberSaveable { mutableStateOf(false) }

        if (boostDialogOpen) {
            val state by Database.loudnessBoost(mediaItem.mediaId).collectAsState(initial = null)
            var newValue by remember(state) { mutableFloatStateOf(state ?: 0f) }

            fun submit() = query {
                Database.setLoudnessBoost(mediaItem.mediaId, if (newValue == 0f) null else newValue)
            }

            SliderDialog(
                onDismiss = {
                    boostDialogOpen = false
                    submit()
                },
                title = stringResource(R.string.song_volume_boost),
                state = newValue * 100f,
                setState = { newValue = it / 100f },
                onSlide = { },
                onSlideCompleted = { submit() },
                min = -2000f,
                max = 2000f,
                toDisplay = {
                    stringResource(R.string.format_db, "%.2f".format(it.roundToInt() / 100f))
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.reset),
                        onClick = {
                            newValue = 0f
                            submit()
                        }
                    )
                }
            }
        }

        with(PlayerPreferences) {
            val actions: @Composable () -> Unit = {
                IconButton(
                    onClick = { speedDialogOpen = true },
                    icon = R.drawable.speed,
                    color = colorPalette.text,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .size(20.dp)
                )

                IconButton(
                    onClick = { boostDialogOpen = true },
                    icon = R.drawable.volume_up,
                    color = colorPalette.text,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .size(20.dp)
                )
            }

            Queue(
                layoutState = playerBottomSheetState,
                beforeContent = {
                    when (playerLayout) {
                        PlayerPreferences.PlayerLayout.Classic -> actions()

                        PlayerPreferences.PlayerLayout.New -> TextToggle(
                            state = trackLoopEnabled,
                            toggleState = { trackLoopEnabled = !trackLoopEnabled },
                            name = stringResource(R.string.song_loop)
                        )
                    }
                },
                afterContent = {
                    if (playerLayout == PlayerPreferences.PlayerLayout.New) actions()

                    IconButton(
                        icon = R.drawable.ellipsis_horizontal,
                        color = colorPalette.text,
                        onClick = {
                            menuState.display {
                                PlayerMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = mediaItem,
                                    binder = binder
                                )
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .size(20.dp)
                    )
                },
                backgroundColorProvider = { colorPalette.background2 },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@kotlin.OptIn(ExperimentalAnimationApi::class)
@Composable
@OptIn(UnstableApi::class)
private fun PlayerMenu(
    binder: PlayerService.Binder,
    mediaItem: MediaItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onStartRadio = {
            binder.stopRadio()
            binder.player.seamlessPlay(mediaItem)
            binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
        },
        onGoToEqualizer = {
            try {
                activityResultLauncher.launch(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder.player.audioSessionId)
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                )
            } catch (e: ActivityNotFoundException) {
                context.toast(context.getString(R.string.no_equalizer_installed))
            }
        },
        onShowSleepTimer = {},
        onDismiss = onDismiss
    )
}
