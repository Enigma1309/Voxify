package com.exilonium.voxify.ui.screens.pipedplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import io.ktor.http.Url
import com.exilonium.compose.persist.PersistMapCleanup
import com.exilonium.compose.routing.RouteHandler
import com.exilonium.piped.models.authenticatedWith
import com.exilonium.voxify.R
import com.exilonium.voxify.ui.components.themed.Scaffold
import com.exilonium.voxify.ui.screens.GlobalRoutes
import com.exilonium.voxify.ui.screens.Route
import java.util.UUID

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Route
@Composable
fun PipedPlaylistScreen(
    apiBaseUrl: Url,
    sessionToken: String,
    playlistId: UUID
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val session by remember { derivedStateOf { apiBaseUrl authenticatedWith sessionToken } }

    PersistMapCleanup(tagPrefix = "pipedplaylist/$playlistId")

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
                        0 -> PipedPlaylistSongList(
                            session = session,
                            playlistId = playlistId
                        )
                    }
                }
            }
        }
    }
}
