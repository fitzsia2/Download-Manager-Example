package com.example.downloadmanagerexample.features.photos.domain

sealed interface PhotoCacheState {

    val metadata: RemotePhotoMetadata

    class NotCached(override val metadata: RemotePhotoMetadata) : PhotoCacheState

    class Cached(override val metadata: RemotePhotoMetadata, val photo: RemotePhoto) : PhotoCacheState

    class Error(override val metadata: RemotePhotoMetadata) : PhotoCacheState

    class Downloading(override val metadata: RemotePhotoMetadata) : PhotoCacheState
}
