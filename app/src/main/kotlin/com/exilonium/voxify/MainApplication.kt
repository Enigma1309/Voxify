package com.exilonium.voxify

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.util.DebugLogger
import com.exilonium.compose.persist.PersistMap
import com.exilonium.compose.persist.PersistMapOwner
import com.exilonium.voxify.preferences.DataPreferences
import androidx.work.Configuration as WorkManagerConfiguration

val globalPersistMap = PersistMap()

class MainApplication : Application(), ImageLoaderFactory, PersistMapOwner, WorkManagerConfiguration.Provider {
    override fun onCreate() {
        super.onCreate()
        Dependencies.init(this)
        DatabaseInitializer()
    }

    override fun newImageLoader() = ImageLoader.Builder(this)
        .crossfade(true)
        .respectCacheHeaders(false)
        .diskCache(
            DiskCache.Builder()
                .directory(cacheDir.resolve("coil"))
                .maxSizeBytes(DataPreferences.coilDiskCacheMaxSize.bytes)
                .build()
        )
        .let { if (BuildConfig.DEBUG) it.logger(DebugLogger()) else it }
        .build()

    override val persistMap = globalPersistMap

    override val workManagerConfiguration = WorkManagerConfiguration.Builder()
        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
        .build()
}
