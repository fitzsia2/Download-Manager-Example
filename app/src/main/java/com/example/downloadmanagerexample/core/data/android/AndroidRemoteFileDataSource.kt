package com.example.downloadmanagerexample.core.data.android

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import androidx.core.content.getSystemService
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.example.downloadmanagerexample.R
import com.example.downloadmanagerexample.core.data.RemoteFileDataSource
import com.example.downloadmanagerexample.core.domain.DownloadError
import com.example.downloadmanagerexample.core.utils.AppDispatchers
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.DownloadedFile
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.seconds

class AndroidRemoteFileDataSource(
    private val context: Context,
    private val appDispatchers: AppDispatchers
) : RemoteFileDataSource {

    private val downloadManager = context.getSystemService<DownloadManager>()!!

    /**
     * Dummy implementation for fetching metadata for remote files
     */
    override suspend fun getRemoteFileMetadata(): List<RemoteFileMetadata> {
        delay(2.seconds)
        val metadata = listOf(
            RemoteFileMetadata("Bird", "https://www.allaboutbirds.org/news/wp-content/uploads/2020/07/STanager-Shapiro-ML.jpg"),
            RemoteFileMetadata("Rohlik", "https://www.nopek.cz/userfiles/photogallery/big/rohlik-standard-43g__mi001-251.jpg"),
            RemoteFileMetadata("1GB", "https://speed.hetzner.de/1GB.bin"),
            RemoteFileMetadata("10GB file", "https://speed.hetzner.de/10GB.bin"),
        )
        return metadata
    }

    override fun downloadToFile(uri: String, file: File) {
        val request = createDownloadRequest(uri, file)
        downloadManager.enqueue(request)
    }

    private fun createDownloadRequest(uri: String, file: File): DownloadManager.Request {
        return DownloadManager.Request(uri.toUri()).apply {
            val title = context.getString(R.string.download_manager_download_in_progress, file.name)
            setTitle(title)
            setDestinationUri(file.toUri())
        }
    }

    override suspend fun synchronize(metadata: List<RemoteFileMetadata>) {
        return withIoDispatcher {
            val downloads = getDownloadManagerDownloads()
            removeUnrecognizedDownloads(downloads, metadata)
        }
    }

    private fun getDownloadManagerDownloads(): List<RemoteDownload> {
        val cursor = downloadManager.query(DownloadManager.Query())
        val downloads = buildList {
            while (cursor.moveToNext()) {
                RemoteDownload.Factory(cursor).create()
                    ?.let { add(it) }
            }
        }
        cursor.close()
        return downloads
    }

    private suspend fun removeUnrecognizedDownloads(downloads: List<RemoteDownload>, metadata: List<RemoteFileMetadata>) {
        downloads.forEach { download ->
            val metadatum = metadata.find { metadatum -> metadatum.uri == download.remoteUri }
            if (metadatum == null) {
                removeDownload(download.remoteUri)
            }
        }
    }

    private fun createCachedFileState(remoteDownload: RemoteDownload?, metadata: RemoteFileMetadata): CachedFileState {
        return when (remoteDownload) {
            is RemoteDownload.Failed -> CachedFileState.Error(metadata, remoteDownload.cause)
            is RemoteDownload.Paused,
            is RemoteDownload.Pending,
            is RemoteDownload.Running -> CachedFileState.Downloading(metadata)
            is RemoteDownload.Successful -> CachedFileState.Cached(metadata, DownloadedFile(metadata.name, remoteDownload.localUri.toUri().toFile()))
            null -> CachedFileState.NotCached(metadata)
        }
    }

    override suspend fun getCachedFileState(metadata: RemoteFileMetadata): CachedFileState {
        return withIoDispatcher {
            val remoteDownload = getDownloadManagerDownloads()
                .find { download -> download.remoteUri == metadata.uri }
            createCachedFileState(remoteDownload, metadata)
        }
    }

    override suspend fun removeDownload(remoteUri: String) {
        return withIoDispatcher {
            getDownloadManagerDownloads()
                .filter { download -> download.remoteUri == remoteUri }
                .forEach { download -> downloadManager.remove(download.id) }
        }
    }

    private suspend fun <T> withIoDispatcher(block: suspend CoroutineScope.() -> T): T {
        return withContext(appDispatchers.io) { block() }
    }

    private sealed interface RemoteDownload {

        val id: Long

        val remoteUri: String

        data class Pending(
            override val id: Long,
            override val remoteUri: String,
        ) : RemoteDownload

        data class Running(
            override val id: Long,
            override val remoteUri: String,
        ) : RemoteDownload

        data class Paused(
            override val id: Long,
            override val remoteUri: String,
        ) : RemoteDownload

        data class Successful(
            override val id: Long,
            override val remoteUri: String,
            val localUri: String,
        ) : RemoteDownload

        data class Failed(
            override val id: Long,
            override val remoteUri: String,
            val cause: DownloadError,
        ) : RemoteDownload

        @SuppressLint("Range")
        class Factory(private val cursor: Cursor) {

            private val uri get() = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI))

            private val id get() = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))

            private val state get() = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

            private val localFileUri get() = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))

            private val error: DownloadError
                get() {
                    val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                    return when (reason) {
                        DownloadManager.ERROR_UNKNOWN -> DownloadError.Unknown
                        DownloadManager.ERROR_FILE_ERROR -> DownloadError.FileDownloadError
                        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> DownloadError.UnhandledHttpCode
                        DownloadManager.ERROR_HTTP_DATA_ERROR -> DownloadError.HttpData
                        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> DownloadError.TooManyRedirects
                        DownloadManager.ERROR_INSUFFICIENT_SPACE -> DownloadError.InsufficientSpace
                        DownloadManager.ERROR_DEVICE_NOT_FOUND -> DownloadError.DeviceNotFound
                        DownloadManager.ERROR_CANNOT_RESUME -> DownloadError.CannotResume
                        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> DownloadError.FileAlreadyExists
                        else -> DownloadError.Http(reason)
                    }
                }

            fun create(): RemoteDownload? {
                return when (state) {
                    DownloadManager.STATUS_PENDING -> Pending(id, uri)
                    DownloadManager.STATUS_RUNNING -> Running(id, uri)
                    DownloadManager.STATUS_PAUSED -> Paused(id, uri)
                    DownloadManager.STATUS_SUCCESSFUL -> Successful(id, uri, localFileUri)
                    DownloadManager.STATUS_FAILED -> Failed(id, uri, error)
                    else -> null
                }
            }
        }
    }
}
