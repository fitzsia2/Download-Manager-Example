package com.example.downloadmanagerexample.features.photos.domain

import kotlinx.coroutines.flow.Flow

interface PhotoRepository {

    val remotePhotoMetadataCacheStateStream: Flow<RemotePhotoMetadataCacheState>

    fun downloadPhoto(remotePhotoMetadata: RemotePhotoMetadata)

    suspend fun deletePhoto(remotePhotoMetadata: PhotoCacheState.Cached)
}
