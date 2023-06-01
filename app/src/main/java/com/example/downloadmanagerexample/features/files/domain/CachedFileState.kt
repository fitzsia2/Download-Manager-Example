package com.example.downloadmanagerexample.features.files.domain

sealed interface CachedFileState {

    val metadata: RemoteFileMetadata

    class NotCached(override val metadata: RemoteFileMetadata) : CachedFileState

    class Cached(override val metadata: RemoteFileMetadata, val file: RemoteFile) : CachedFileState

    class Error(override val metadata: RemoteFileMetadata) : CachedFileState

    class Downloading(override val metadata: RemoteFileMetadata) : CachedFileState
}
