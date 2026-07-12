package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(UnstableApi::class)
object VideoCacheManager {
    private var simpleCache: SimpleCache? = null
    private var cacheDir: File? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        val dir = File(context.cacheDir, "exo_video_cache")
        cacheDir = dir
        
        // Clean up files older than 1 hour before using the cache
        cleanOldCacheFiles(dir)

        if (simpleCache == null) {
            val databaseProvider = StandaloneDatabaseProvider(context)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            // 512 MB max local cache size
            val evictor = LeastRecentlyUsedCacheEvictor(512 * 1024 * 1024L)
            simpleCache = SimpleCache(dir, evictor, databaseProvider)
        }
        return simpleCache!!
    }

    /**
     * Recursively deletes files older than 1 hour from the cache directory
     */
    private fun cleanOldCacheFiles(dir: File) {
        if (!dir.exists()) return
        val oneHourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        try {
            dir.walkBottomUp().forEach { file ->
                if (file.isFile) {
                    if (file.lastModified() < oneHourAgo) {
                        file.delete()
                    }
                } else if (file.isDirectory && file != dir) {
                    // Delete empty subdirectories
                    val files = file.listFiles()
                    if (files == null || files.isEmpty()) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Schedules or manually fires a background flush.
     */
    fun performPeriodicCleanup(context: Context) {
        val dir = cacheDir ?: File(context.cacheDir, "exo_video_cache")
        cleanOldCacheFiles(dir)
    }

    @Synchronized
    fun createDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private val preloadScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    /**
     * Silently pre-buffers/pre-caches the next video in the background to enable zero-latency scrolling
     */
    fun preloadVideo(context: Context, videoUrl: String) {
        if (videoUrl.isBlank()) return
        preloadScope.launch {
            try {
                val cache = getCache(context)
                val cacheDataSourceFactory = createDataSourceFactory(context)
                val dataSource = cacheDataSourceFactory.createDataSource()
                // Preload the first 1 MB of the video for immediate playback start
                val dataSpec = androidx.media3.datasource.DataSpec(Uri.parse(videoUrl), 0, 1024 * 1024L)
                val cacheWriter = CacheWriter(
                    dataSource,
                    dataSpec,
                    null,
                    null
                )
                cacheWriter.cache()
            } catch (e: Exception) {
                // Ignore errors during preloading
            }
        }
    }
}
