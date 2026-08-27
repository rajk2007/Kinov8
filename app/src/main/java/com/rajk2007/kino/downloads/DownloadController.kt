package com.rajk2007.kino.downloads

import android.app.Notification
import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.rajk2007.kino.R
import com.rajk2007.kino.core.ExtractorLink
import com.rajk2007.kino.core.LoadResponse
import java.io.File
import java.util.concurrent.Executors

object DownloadController {
    fun enqueue(title: String, link: ExtractorLink) {
        val context = AppContextHolder.context ?: return
        val id = "KINO:$title:${link.url.hashCode()}"
        val mimeType = if (link.type == com.rajk2007.kino.core.StreamType.M3U8) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4
        DownloadService.sendAddDownload(
            context,
            KinoDownloadService::class.java,
            DownloadRequest.Builder(id, Uri.parse(link.url))
                .setMimeType(mimeType)
                .build(),
            false
        )
    }
}

object AppContextHolder { var context: Context? = null }

object DownloadRepository {
    fun snapshot(context: Context): List<Download> {
        val cursor = KinoDownloadService.managerFor(context).downloadIndex.getDownloads()
        return cursor.use {
            buildList { while (it.moveToNext()) add(it.download) }
        }
    }
}

class KinoDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.app_name,
    0
) {
    override fun getDownloadManager(): DownloadManager = managerFor(this)

    override fun getScheduler(): Scheduler? = if (android.os.Build.VERSION.SDK_INT >= 21) {
        PlatformScheduler(this, JOB_ID)
    } else null

    override fun getForegroundNotification(downloads: List<Download>, notMetRequirements: Int): Notification =
        DownloadNotificationHelper(this, CHANNEL_ID).buildProgressNotification(
            this, R.drawable.ic_download, null, "KINO downloads", downloads, notMetRequirements
        )

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val JOB_ID = 1002
        private const val CHANNEL_ID = "kino_downloads"
        @Volatile private var manager: DownloadManager? = null

        internal fun managerFor(context: Context): DownloadManager = manager ?: synchronized(this) {
            manager ?: run {
                val database = StandaloneDatabaseProvider(context)
                val cache = SimpleCache(File(context.cacheDir, "kino-downloads"), NoOpCacheEvictor(), database)
                val upstream = DefaultHttpDataSource.Factory()
                val cacheDataSource = CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(upstream)
                val executor = Executors.newFixedThreadPool(3)
                DownloadManager(context, database, cache, cacheDataSource, executor).also { manager = it }
            }
        }
    }
}
