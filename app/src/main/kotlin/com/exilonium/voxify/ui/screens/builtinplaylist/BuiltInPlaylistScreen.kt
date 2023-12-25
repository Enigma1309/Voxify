package com.exilonium.voxify.ui.screens.builtinplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import com.exilonium.compose.persist.PersistMapCleanup
import com.exilonium.compose.routing.RouteHandler
import com.exilonium.voxify.R
import com.exilonium.voxify.enums.BuiltInPlaylist
import com.exilonium.voxify.preferences.DataPreferences
import com.exilonium.voxify.ui.components.themed.Scaffold
import com.exilonium.voxify.ui.screens.GlobalRoutes
import com.exilonium.voxify.ui.screens.Route

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Route
@Composable
fun BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabIndexChanged) = rememberSaveable {
        mutableIntStateOf(
            when (builtInPlaylist) {
                BuiltInPlaylist.Favorites -> 0
                BuiltInPlaylist.Offline -> 1
                BuiltInPlaylist.Top -> 2
            }
        )
    }

    PersistMapCleanup(tagPrefix = "${builtInPlaylist.name}/")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChanged = onTabIndexChanged,
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.favorites), R.drawable.heart)
                    item(1, stringResource(R.string.offline), R.drawable.airplane)
                    item(
                        2,
                        stringResource(R.string.format_top_playlist, DataPreferences.topListLength),
                        R.drawable.trending_up
                    )
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Favorites)
                        1 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Offline)
                        2 -> BuiltInPlaylistSongs(builtInPlaylist = BuiltInPlaylist.Top)
                    }
                }
            }
        }
    }
}
