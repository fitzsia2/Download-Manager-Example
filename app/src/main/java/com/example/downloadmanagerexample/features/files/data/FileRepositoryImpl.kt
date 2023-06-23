package com.example.downloadmanagerexample.features.files.data

import com.example.downloadmanagerexample.core.data.LocalFileDataSource
import com.example.downloadmanagerexample.core.data.RemoteFileDataSource
import com.example.downloadmanagerexample.core.utils.AppCoroutineScope
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.FileRepository
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlin.time.Duration.Companion.milliseconds

class FileRepositoryImpl(
    private val appCoroutineScope: AppCoroutineScope,
    private val localFileDataSource: LocalFileDataSource,
    private val remoteFileDataSource: RemoteFileDataSource,
) : FileRepository {

    override suspend fun getAvailableDownloads(): List<RemoteFileMetadata> {
        return remoteFileDataSource.getRemoteFileMetadata()
    }

    override suspend fun synchronize(metadata: List<RemoteFileMetadata>) {
        remoteFileDataSource.synchronize(metadata)
    }

    override fun getCachedFileStateStream(metadata: List<RemoteFileMetadata>): Flow<List<CachedFileState>> {
        return flow {
            while (true) {
                val cacheStates = metadata.map { getCacheState(it) }
                emit(cacheStates)
                delay(DOWNLOAD_STATE_POLL_PERIOD)
            }
        }
            .distinctUntilChanged()
            .shareIn(
                scope = appCoroutineScope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )
    }

    private suspend fun getCacheState(metadata: RemoteFileMetadata): CachedFileState {
        return remoteFileDataSource.getCachedFileState(metadata)
    }

    override fun downloadFile(cachedFileState: CachedFileState) {
        val metadatum = cachedFileState.metadata
        val file = localFileDataSource.getDownloadedFile(metadatum.name)
        remoteFileDataSource.downloadToFile(metadatum.uri, file)
    }

    override suspend fun deleteFile(cachedFileState: CachedFileState) {
        if (cachedFileState is CachedFileState.Cached) {
            localFileDataSource.deleteFile(cachedFileState.downloadedFile.file)
        }
        remoteFileDataSource.removeDownload(cachedFileState.metadata.uri)
    }

    companion object {

        private val DOWNLOAD_STATE_POLL_PERIOD = 2000.milliseconds
    }
}
