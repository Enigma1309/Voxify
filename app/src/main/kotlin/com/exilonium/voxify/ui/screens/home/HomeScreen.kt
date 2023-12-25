package com.exilonium.voxify.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import com.exilonium.compose.persist.PersistMapCleanup
import com.exilonium.compose.routing.RouteHandler
import com.exilonium.compose.routing.defaultStacking
import com.exilonium.compose.routing.defaultStill
import com.exilonium.compose.routing.defaultUnstacking
import com.exilonium.compose.routing.isStacking
import com.exilonium.compose.routing.isUnknown
import com.exilonium.compose.routing.isUnstacking
import com.exilonium.voxify.Database
import com.exilonium.voxify.R
import com.exilonium.voxify.models.SearchQuery
import com.exilonium.voxify.models.toUiMood
import com.exilonium.voxify.preferences.DataPreferences
import com.exilonium.voxify.preferences.UIStatePreferences
import com.exilonium.voxify.query
import com.exilonium.voxify.ui.components.themed.Scaffold
import com.exilonium.voxify.ui.screens.GlobalRoutes
import com.exilonium.voxify.ui.screens.Route
import com.exilonium.voxify.ui.screens.albumRoute
import com.exilonium.voxify.ui.screens.artistRoute
import com.exilonium.voxify.ui.screens.builtInPlaylistRoute
import com.exilonium.voxify.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import com.exilonium.voxify.ui.screens.localPlaylistRoute
import com.exilonium.voxify.ui.screens.localplaylist.LocalPlaylistScreen
import com.exilonium.voxify.ui.screens.moodRoute
import com.exilonium.voxify.ui.screens.pipedPlaylistRoute
import com.exilonium.voxify.ui.screens.playlistRoute
import com.exilonium.voxify.ui.screens.search.SearchScreen
import com.exilonium.voxify.ui.screens.searchResultRoute
import com.exilonium.voxify.ui.screens.searchRoute
import com.exilonium.voxify.ui.screens.searchresult.SearchResultScreen
import com.exilonium.voxify.ui.screens.settings.SettingsScreen
import com.exilonium.voxify.ui.screens.settingsRoute

@OptIn(ExperimentalAnimationApi::class)
@Route
@Composable
fun HomeScreen(onPlaylistUrl: (String) -> Unit) {
    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup("home/")

    RouteHandler(
        listenToGlobalEmitter = true,
        transitionSpec = {
            when {
                isStacking -> defaultStacking
                isUnstacking -> defaultUnstacking
                isUnknown -> when {
                    initialState.route == searchRoute && targetState.route == searchResultRoute -> defaultStacking
                    initialState.route == searchResultRoute && targetState.route == searchRoute -> defaultUnstacking
                    else -> defaultStill
                }

                else -> defaultStill
            }
        }
    ) {
        GlobalRoutes()

        settingsRoute {
            SettingsScreen()
        }

        localPlaylistRoute { playlistId ->
            LocalPlaylistScreen(
                playlistId = playlistId ?: error("playlistId cannot be null")
            )
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(
                builtInPlaylist = builtInPlaylist
            )
        }

        searchResultRoute { query ->
            SearchResultScreen(
                query = query,
                onSearchAgain = { searchRoute(query) }
            )
        }

        searchRoute { initialTextInput ->
            SearchScreen(
                initialTextInput = initialTextInput,
                onSearch = { query ->
                    pop()
                    searchResultRoute(query)

                    if (!DataPreferences.pauseSearchHistory) query {
                        Database.insert(SearchQuery(query = query))
                    }
                },
                onViewPlaylist = onPlaylistUrl
            )
        }

        NavHost {
            Scaffold(
                topIconButtonId = R.drawable.settings,
                onTopIconButtonClick = { settingsRoute() },
                tabIndex = UIStatePreferences.homeScreenTabIndex,
                onTabChanged = { UIStatePreferences.homeScreenTabIndex = it },
                tabColumnContent = { item ->
                    item(0, stringResource(R.string.quick_picks), R.drawable.sparkles)
                    item(1, stringResource(R.string.discover), R.drawable.globe)
                    item(2, stringResource(R.string.songs), R.drawable.musical_notes)
                    item(3, stringResource(R.string.playlists), R.drawable.playlist)
                    item(4, stringResource(R.string.artists), R.drawable.person)
                    item(5, stringResource(R.string.albums), R.drawable.disc)
                    item(6, stringResource(R.string.local), R.drawable.download)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    val onSearchClick = { searchRoute("") }
                    when (currentTabIndex) {
                        0 -> QuickPicks(
                            onAlbumClick = { albumRoute(it) },
                            onArtistClick = { artistRoute(it) },
                            onPlaylistClick = { playlistRoute(it) },
                            onSearchClick = onSearchClick
                        )

                        1 -> HomeDiscovery(
                            onMoodClick = { mood -> moodRoute(mood.toUiMood()) },
                            onNewReleaseAlbumClick = { albumRoute(it) },
                            onSearchClick = onSearchClick
                        )

                        2 -> HomeSongs(
                            onSearchClick = onSearchClick
                        )

                        3 -> HomePlaylists(
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onPipedPlaylistClick = { session, playlist ->
                                pipedPlaylistRoute(
                                    p0 = session.apiBaseUrl.toString(),
                                    p1 = session.token,
                                    p2 = playlist.id.toString()
                                )
                            },
                            onSearchClick = onSearchClick
                        )

                        4 -> HomeArtistList(
                            onArtistClick = { artistRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        5 -> HomeAlbums(
                            onAlbumClick = { albumRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        6 -> HomeLocalSongs(
                            onSearchClick = onSearchClick
                        )
                    }
                }
            }
        }
    }
}
