package com.example.downloadmanagerexample.features.photos.data

import androidx.core.net.toUri
import com.example.downloadmanagerexample.core.data.LocalFileDataSource
import com.example.downloadmanagerexample.core.data.RemoteFileDataSource
import com.example.downloadmanagerexample.core.utils.AppCoroutineScope
import com.example.downloadmanagerexample.features.photos.domain.PhotoCacheState
import com.example.downloadmanagerexample.features.photos.domain.PhotoRepository
import com.example.downloadmanagerexample.features.photos.domain.RemotePhoto
import com.example.downloadmanagerexample.features.photos.domain.RemotePhotoMetadata
import com.example.downloadmanagerexample.features.photos.domain.RemotePhotoMetadataCacheState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

class PhotoRepositoryImpl(
    private val appCoroutineScope: AppCoroutineScope,
    private val localFileDataSource: LocalFileDataSource,
    private val remoteFileDataSource: RemoteFileDataSource,
) : PhotoRepository {

    override val remotePhotoMetadataCacheStateStream =
        MutableStateFlow<RemotePhotoMetadataCacheState>(RemotePhotoMetadataCacheState.Synchronizing)

    init {
        appCoroutineScope.launch {
            val metadata = getRemotePhotoMetadata()
            val cachedStates = getCacheStates(metadata)
            cachedStates.filterIsInstance<PhotoCacheState.Downloading>().forEach {
                launchDownloadObserver(it.metadata)
            }
            remotePhotoMetadataCacheStateStream.value = RemotePhotoMetadataCacheState.Synchronized(cachedStates)
        }
    }

    /**
     * Dummy method for fetching metadata for remote photos
     */
    private suspend fun getRemotePhotoMetadata(): List<RemotePhotoMetadata> {
        delay(2.seconds)
        val metadata = listOf(
            RemotePhotoMetadata("Bird", "https://www.allaboutbirds.org/news/wp-content/uploads/2020/07/STanager-Shapiro-ML.jpg".toUri()),
            RemotePhotoMetadata("Rohlik", "https://www.nopek.cz/userfiles/photogallery/big/rohlik-standard-43g__mi001-251.jpg".toUri()),
        )
        return metadata
    }

    private suspend fun getCacheStates(remoteMapMetadataList: List<RemotePhotoMetadata>): List<PhotoCacheState> {
        return remoteMapMetadataList.map { getCacheState(it) }
    }

    private suspend fun getCacheState(mapMetadata: RemotePhotoMetadata): PhotoCacheState {
        val file = localFileDataSource.getDownloadedFile(mapMetadata.name)
        return when {
            remoteFileDataSource.isDownloadInProgress(mapMetadata.uri, file) -> PhotoCacheState.Downloading(mapMetadata)
            file.length() > 0 -> PhotoCacheState.Cached(mapMetadata, RemotePhoto(mapMetadata.name, file))
            else -> PhotoCacheState.NotCached(mapMetadata)
        }
    }

    override fun downloadPhoto(remotePhotoMetadata: RemotePhotoMetadata) {
        val file = localFileDataSource.getDownloadedFile(remotePhotoMetadata.name)
        remoteFileDataSource.downloadToFile(remotePhotoMetadata.uri, file)
        val updatedState = PhotoCacheState.Downloading(remotePhotoMetadata)
        updateCache(remotePhotoMetadata, updatedState)

        launchDownloadObserver(remotePhotoMetadata)
    }

    private fun launchDownloadObserver(mapMetadata: RemotePhotoMetadata) {
        appCoroutineScope.launch {
            while (true) {
                when (val cacheState = getCacheState(mapMetadata)) {
                    is PhotoCacheState.Downloading -> delay(DOWNLOAD_STATE_POLL_PERIOD)
                    is PhotoCacheState.Error -> {
                        Timber.e(Exception(cacheState.metadata.toString()))
                        break
                    }
                    is PhotoCacheState.NotCached,
                    is PhotoCacheState.Cached -> {
                        updateCache(mapMetadata, cacheState)
                        break
                    }
                }
            }
        }
    }

    private fun updateCache(metadata: RemotePhotoMetadata, cacheState: PhotoCacheState) {
        (remotePhotoMetadataCacheStateStream.value as? RemotePhotoMetadataCacheState.Synchronized)?.photoCacheStates?.let { maps ->
            val mutableList = maps.toMutableList()
            val index = maps.indexOfFirst { it.metadata == metadata }
            mutableList[index] = cacheState
            remotePhotoMetadataCacheStateStream.value = RemotePhotoMetadataCacheState.Synchronized(mutableList)
        }
    }

    override fun deletePhoto(remotePhotoMetadata: PhotoCacheState.Cached) {
        localFileDataSource.deleteFile(remotePhotoMetadata.photo.file)
        updateCache(remotePhotoMetadata.metadata, PhotoCacheState.NotCached(remotePhotoMetadata.metadata))
    }

    companion object {

        private val DOWNLOAD_STATE_POLL_PERIOD = 1.seconds
    }
}
