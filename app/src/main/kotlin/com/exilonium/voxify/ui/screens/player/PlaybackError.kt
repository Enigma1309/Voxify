package com.exilonium.voxify.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.exilonium.voxify.ui.styling.LocalAppearance
import com.exilonium.voxify.ui.styling.PureBlackColorPalette
import com.exilonium.voxify.utils.center
import com.exilonium.voxify.utils.color
import com.exilonium.voxify.utils.medium

@Composable
fun PlaybackError(
    isDisplayed: Boolean,
    messageProvider: @Composable () -> String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) = Box {
    val (_, typography) = LocalAppearance.current

    val message by rememberUpdatedState(newValue = messageProvider())

    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Spacer(
            modifier = modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onDismiss()
                        }
                    )
                }
                .fillMaxSize()
                .background(Color.Black.copy(0.8f))
        )
    }

    AnimatedVisibility(
        visible = isDisplayed,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        BasicText(
            text = message,
            style = typography.xs.center.medium.color(PureBlackColorPalette.text),
            modifier = Modifier
                .background(Color.Black.copy(0.4f))
                .padding(all = 8.dp)
                .fillMaxWidth()
        )
    }
}
