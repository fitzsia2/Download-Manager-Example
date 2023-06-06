package com.example.downloadmanagerexample.core.data.fake

import com.example.downloadmanagerexample.core.data.RemoteFileDataSource
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadata
import java.io.File

internal class FakeRemoteFileDataSource : RemoteFileDataSource {

    var remoteFileMetadata: List<RemoteFileMetadata> = emptyList()

    var cachedFileStates: List<CachedFileState> = emptyList()

    override suspend fun getRemoteFileMetadata(): List<RemoteFileMetadata> {
        return remoteFileMetadata
    }

    override fun downloadToFile(uri: String, file: File) {
    }

    override suspend fun synchronize(metadata: List<RemoteFileMetadata>): List<CachedFileState> {
        return metadata.map { metadatum ->
            val cachedFileState = cachedFileStates.find { it.metadata == metadatum }
            cachedFileState ?: CachedFileState.NotCached(metadatum)
        }
    }

    override suspend fun getCachedFileState(metadata: RemoteFileMetadata): CachedFileState {
        return cachedFileStates.find { it.metadata == metadata } ?: CachedFileState.NotCached(metadata)
    }

    override suspend fun removeDownload(remoteUri: String) = Unit
}
