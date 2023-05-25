package com.example.downloadmanagerexample.features.photos.domain

sealed interface RemotePhotoMetadataCacheState {

    object Synchronizing : RemotePhotoMetadataCacheState

    data class Synchronized(val photoCacheStates: List<PhotoCacheState>) : RemotePhotoMetadataCacheState

    data class Error(val remotePhotos: List<RemotePhoto>) : RemotePhotoMetadataCacheState
}
