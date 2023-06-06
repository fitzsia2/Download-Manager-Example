package com.example.downloadmanagerexample.features.files.domain

sealed interface RemoteFileMetadataCacheState {

    object Synchronizing : RemoteFileMetadataCacheState

    data class Synchronized(val cachedFileStates: List<CachedFileState>) : RemoteFileMetadataCacheState

    data class Error(val downloadedFiles: List<DownloadedFile>) : RemoteFileMetadataCacheState
}
