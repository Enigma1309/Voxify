package com.exilonium.voxify.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import com.exilonium.compose.persist.PersistMapCleanup
import com.exilonium.compose.routing.RouteHandler
import com.exilonium.voxify.R
import com.exilonium.voxify.ui.components.themed.Scaffold
import com.exilonium.voxify.ui.screens.GlobalRoutes
import com.exilonium.voxify.ui.screens.Route

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Route
@Composable
fun PlaylistScreen(
    browseId: String,
    params: String?,
    maxDepth: Int? = null
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    PersistMapCleanup(tagPrefix = "playlist/$browseId")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChanged = { },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.songs), R.drawable.musical_notes)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> PlaylistSongList(
                            browseId = browseId,
                            params = params,
                            maxDepth = maxDepth
                        )
                    }
                }
            }
        }
    }
}
