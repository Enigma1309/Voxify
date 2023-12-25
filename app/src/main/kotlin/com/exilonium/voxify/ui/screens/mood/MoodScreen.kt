package com.exilonium.voxify.ui.screens.mood

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import com.exilonium.compose.persist.PersistMapCleanup
import com.exilonium.compose.routing.RouteHandler
import com.exilonium.voxify.R
import com.exilonium.voxify.models.Mood
import com.exilonium.voxify.ui.components.themed.Scaffold
import com.exilonium.voxify.ui.screens.GlobalRoutes
import com.exilonium.voxify.ui.screens.Route

@OptIn(ExperimentalAnimationApi::class)
@Route
@Composable
fun MoodScreen(mood: Mood) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(tagPrefix = "playlist/$DEFAULT_BROWSE_ID")

    RouteHandler(listenToGlobalEmitter = true) {
        GlobalRoutes()

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChanged = { },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.mood), R.drawable.disc)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> MoodList(mood = mood)
                    }
                }
            }
        }
    }
}
