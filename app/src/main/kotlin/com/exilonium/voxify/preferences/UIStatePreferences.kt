package com.exilonium.voxify.preferences

import com.exilonium.voxify.GlobalPreferencesHolder

object UIStatePreferences : GlobalPreferencesHolder() {
    var homeScreenTabIndex by int(0)
    var searchResultScreenTabIndex by int(0)
    var artistScreenTabIndex by int(0)
}
