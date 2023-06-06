package com.example.downloadmanagerexample.core.data.android

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
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
import kotlinx.coroutines.withContext
import java.io.File

class AndroidRemoteFileDataSource(
    private val context: Context,
    private val appDispatchers: AppDispatchers
) : RemoteFileDataSource {

    private val downloadManager = context.getSystemService<DownloadManager>()!!

    override fun downloadToFile(uri: Uri, file: File) {
        val request = createDownloadRequest(uri, file)
        downloadManager.enqueue(request)
    }

    private fun createDownloadRequest(uri: Uri, file: File): DownloadManager.Request {
        return DownloadManager.Request(uri).apply {
            val title = context.getString(R.string.download_manager_download_in_progress, file.name)
            setTitle(title)
            setDestinationUri(file.toUri())
        }
    }

    override suspend fun synchronize(metadata: List<RemoteFileMetadata>): List<CachedFileState> {
        return withContext(appDispatchers.io) {
            val downloads = getDownloadManagerDownloads()
            removeUnrecognizedDownloads(downloads, metadata)
            metadata.associateWith { metadatum -> downloads.find { it.remoteUri == metadatum.uri } }
                .map { entry -> entry.value.toCachedFileState(entry.key) }
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

    private suspend fun removeUnrecognizedDownloads(
        downloads: List<RemoteDownload>,
        metadata: List<RemoteFileMetadata>
    ) {
        downloads.associateWith { download ->
            metadata.find { metadatum -> metadatum.uri == download.remoteUri }
        }
            .filterValues { it == null }
            .keys
            .forEach { removeDownload(it.remoteUri) }
    }

    private suspend fun RemoteDownload?.toCachedFileState(metadata: RemoteFileMetadata): CachedFileState {
        return when (this) {
            is RemoteDownload.Failed -> {
                removeDownload(remoteUri)
                CachedFileState.Error(metadata, this.cause)
            }
            is RemoteDownload.Paused,
            is RemoteDownload.Pending,
            is RemoteDownload.Running -> CachedFileState.Downloading(metadata)
            is RemoteDownload.Successful -> CachedFileState.Cached(metadata, DownloadedFile(metadata.name, localUri.toFile()))
            null -> CachedFileState.NotCached(metadata)
        }
    }

    override suspend fun getCachedFileState(metadata: RemoteFileMetadata): CachedFileState {
        return getDownloadManagerDownloads()
            .find { it.remoteUri == metadata.uri }
            .toCachedFileState(metadata)
    }

    override suspend fun removeDownload(remoteUri: Uri) {
        return withContext(appDispatchers.io) {
            getDownloadManagerDownloads()
                .filter { download -> download.remoteUri == remoteUri }
                .forEach { download ->
                    downloadManager.remove(download.id)
                }
        }
    }

    private sealed interface RemoteDownload {

        val state: Int

        val id: Long

        val remoteUri: Uri

        data class Pending(
            override val id: Long,
            override val remoteUri: Uri,
            override val state: Int,
        ) : RemoteDownload

        data class Running(
            override val id: Long,
            override val remoteUri: Uri,
            override val state: Int,
        ) : RemoteDownload

        data class Paused(
            override val id: Long,
            override val remoteUri: Uri,
            override val state: Int,
        ) : RemoteDownload

        data class Successful(
            override val id: Long,
            override val remoteUri: Uri,
            override val state: Int,
            val localUri: Uri,
        ) : RemoteDownload

        data class Failed(
            override val id: Long,
            override val remoteUri: Uri,
            override val state: Int,
            val cause: DownloadError,
        ) : RemoteDownload

        class Factory(private val cursor: Cursor) {

            private val uri @SuppressLint("Range") get() = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)).toUri()

            private val id @SuppressLint("Range") get() = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))

            private val state @SuppressLint("Range") get() = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

            private val localFileUri @SuppressLint("Range") get() = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)).toUri()

            private val error: DownloadError
                @SuppressLint("Range")
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
                    DownloadManager.STATUS_PENDING -> Pending(id, uri, state)
                    DownloadManager.STATUS_RUNNING -> Running(id, uri, state)
                    DownloadManager.STATUS_PAUSED -> Paused(id, uri, state)
                    DownloadManager.STATUS_SUCCESSFUL -> Successful(id, uri, state, localFileUri)
                    DownloadManager.STATUS_FAILED -> Failed(id, uri, state, error)
                    else -> null
                }
            }
        }
    }
}
