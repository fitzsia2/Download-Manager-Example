package com.example.downloadmanagerexample.features.files.domain

import com.example.downloadmanagerexample.core.domain.DownloadError

sealed interface CachedFileState {

    val metadata: RemoteFileMetadata

    data class NotCached(override val metadata: RemoteFileMetadata) : CachedFileState

    data class Cached(override val metadata: RemoteFileMetadata, val downloadedFile: DownloadedFile) : CachedFileState

    data class Error(override val metadata: RemoteFileMetadata, val reason: DownloadError) : CachedFileState

    data class Downloading(override val metadata: RemoteFileMetadata) : CachedFileState
}
