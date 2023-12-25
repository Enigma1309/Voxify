package com.exilonium.voxify.preferences

import com.exilonium.voxify.GlobalPreferencesHolder
import com.exilonium.voxify.enums.ColorPaletteMode
import com.exilonium.voxify.enums.ColorPaletteName
import com.exilonium.voxify.enums.ThumbnailRoundness

object AppearancePreferences : GlobalPreferencesHolder() {
    var colorPaletteName by enum(ColorPaletteName.Dynamic)
    var colorPaletteMode by enum(ColorPaletteMode.System)
    var thumbnailRoundness by enum(ThumbnailRoundness.Light)
    var useSystemFont by boolean(false)
    var applyFontPadding by boolean(false)
    var isShowingThumbnailInLockscreen by boolean(false)
    var swipeToHideSong by boolean(false)
}
