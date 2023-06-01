package com.example.downloadmanagerexample.features.files.data

import androidx.core.net.toUri
import com.example.downloadmanagerexample.core.data.LocalFileDataSource
import com.example.downloadmanagerexample.core.data.RemoteFileDataSource
import com.example.downloadmanagerexample.core.utils.AppCoroutineScope
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.FileRepository
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadata
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadataCacheState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class FileRepositoryImpl(
    private val appCoroutineScope: AppCoroutineScope,
    private val localFileDataSource: LocalFileDataSource,
    private val remoteFileDataSource: RemoteFileDataSource,
) : FileRepository {

    override val remoteFileMetadataCacheStateStream =
        MutableStateFlow<RemoteFileMetadataCacheState>(RemoteFileMetadataCacheState.Synchronizing)

    init {
        appCoroutineScope.launch {
            val metadata = getRemoteFileMetadata()
            val cachedStates = remoteFileDataSource.synchronize(metadata)
            cachedStates.filterIsInstance<CachedFileState.Downloading>()
                .forEach { cachedFileState -> launchDownloadObserver(cachedFileState.metadata) }
            remoteFileMetadataCacheStateStream.value = RemoteFileMetadataCacheState.Synchronized(cachedStates)
        }
    }

    /**
     * Dummy method for fetching metadata for remote files
     */
    private suspend fun getRemoteFileMetadata(): List<RemoteFileMetadata> {
        delay(2.seconds)
        val metadata = listOf(
            RemoteFileMetadata("Bird", "https://www.allaboutbirds.org/news/wp-content/uploads/2020/07/STanager-Shapiro-ML.jpg".toUri()),
            RemoteFileMetadata("Rohlik", "https://www.nopek.cz/userfiles/photogallery/big/rohlik-standard-43g__mi001-251.jpg".toUri()),
            RemoteFileMetadata("1GB", "https://speed.hetzner.de/1GB.bin".toUri()),
            RemoteFileMetadata("10GB file", "https://speed.hetzner.de/10GB.bin".toUri()),
        )
        return metadata
    }

    private suspend fun getCacheState(metadata: RemoteFileMetadata): CachedFileState {
        return remoteFileDataSource.getCachedFileState(metadata)
    }

    override fun downloadFile(cachedFileState: CachedFileState) {
        val metadatum = cachedFileState.metadata
        val file = localFileDataSource.getDownloadedFile(metadatum.name)
        remoteFileDataSource.downloadToFile(metadatum.uri, file)
        val updatedState = CachedFileState.Downloading(metadatum)
        updateCache(metadatum, updatedState)

        launchDownloadObserver(metadatum)
    }

    private fun launchDownloadObserver(metadata: RemoteFileMetadata) {
        appCoroutineScope.launch {
            while (true) {
                when (val cacheState = getCacheState(metadata)) {
                    is CachedFileState.Downloading -> delay(DOWNLOAD_STATE_POLL_PERIOD)
                    is CachedFileState.Error -> {
                        updateCache(metadata, cacheState)
                        break
                    }
                    is CachedFileState.NotCached,
                    is CachedFileState.Cached -> {
                        updateCache(metadata, cacheState)
                        break
                    }
                }
            }
        }
    }

    private fun updateCache(metadata: RemoteFileMetadata, cacheState: CachedFileState) {
        (remoteFileMetadataCacheStateStream.value as? RemoteFileMetadataCacheState.Synchronized)?.cachedFileStates?.let { maps ->
            val mutableList = maps.toMutableList()
            val index = maps.indexOfFirst { it.metadata == metadata }
            mutableList[index] = cacheState
            remoteFileMetadataCacheStateStream.value = RemoteFileMetadataCacheState.Synchronized(mutableList)
        }
    }

    override suspend fun deleteFile(cachedFileState: CachedFileState) {
        if (cachedFileState is CachedFileState.Cached) {
            localFileDataSource.deleteFile(cachedFileState.file.file)
        }
        remoteFileDataSource.removeDownload(cachedFileState.metadata.uri)
        updateCache(cachedFileState.metadata, CachedFileState.NotCached(cachedFileState.metadata))
    }

    companion object {

        private val DOWNLOAD_STATE_POLL_PERIOD = 1.seconds
    }
}
