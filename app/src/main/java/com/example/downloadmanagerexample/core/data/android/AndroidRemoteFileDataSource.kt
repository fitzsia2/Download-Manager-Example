package com.example.downloadmanagerexample.core.data.android

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.example.downloadmanagerexample.R
import com.example.downloadmanagerexample.core.data.RemoteFileDataSource
import com.example.downloadmanagerexample.core.utils.AppDispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
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

    override suspend fun isDownloadInProgress(uri: Uri, file: File): Boolean {
        return withContext(appDispatchers.io) {
            val cursor = downloadManager.query(IN_PROGRESS_DOWNLOAD_QUERY)
            while (cursor.moveToNext()) {
                try {
                    val download = RemoteDownload.from(cursor)
                    val matchesTargetDownload = download.uri == uri
                    if (matchesTargetDownload) {
                        cursor.close()
                        return@withContext true
                    }
                } catch (e: RemoteDownload.InvalidCursorIndexException) {
                    Timber.e(e)
                    continue
                }
            }
            cursor.close()
            return@withContext false
        }
    }

    override suspend fun removeDownload(uri: Uri) {
        return withContext(appDispatchers.io) {
            val cursor = downloadManager.query(DownloadManager.Query())
            val matchingDownloads = buildList<RemoteDownload> {
                while (cursor.moveToNext()) {
                    try {
                        val download = RemoteDownload.from(cursor)
                        val matchesTargetDownload = download.uri == uri
                        if (matchesTargetDownload) {
                            add(download)
                        }
                    } catch (e: RemoteDownload.InvalidCursorIndexException) {
                        Timber.e(e)
                        continue
                    }
                }
                cursor.close()
            }
            matchingDownloads.forEach { remoteDownload ->
                downloadManager.remove(remoteDownload.id)
            }
        }
    }

    private data class RemoteDownload(val uri: Uri, val id: Long) {

        class InvalidCursorIndexException(uriIndex: Int) :
            Exception("Download manager returned invalid column indices! uriIndex:$uriIndex")

        companion object {

            @SuppressLint("Range")
            fun from(cursor: Cursor): RemoteDownload {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                if (uriIndex.isInvalid) {
                    throw InvalidCursorIndexException(uriIndex)
                }
                val uri = try {
                    cursor.getString(uriIndex) ?: ""
                } catch (e: Throwable) {
                    ""
                }
                if (uri.isBlank()) {
                    Timber.e("Download uri is empty!")
                }
                val id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID))
                return RemoteDownload(uri.toUri(), id)
            }

            private val Int.isInvalid: Boolean get() = this == -1
        }
    }

    companion object {

        private val IN_PROGRESS_DOWNLOAD_QUERY = DownloadManager.Query()
            .setFilterByStatus(
                DownloadManager.STATUS_PAUSED or
                    DownloadManager.STATUS_PENDING or
                    DownloadManager.STATUS_RUNNING
            )
    }
}
