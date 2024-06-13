package com.example.downloadmanagerexample.features.files.domain

import kotlinx.coroutines.flow.Flow

interface FileRepository {

    suspend fun getAvailableDownloads(): List<RemoteFileMetadata>

    /**
     * Synchronizes [metadata] with local downloads. This will remove any downloads that are not
     * related to [metadata].
     */
    suspend fun synchronize(metadata: List<RemoteFileMetadata>)

    fun getCachedFileStateStream(metadata: List<RemoteFileMetadata>): Flow<List<CachedFileState>>

    fun downloadFile(cachedFileState: CachedFileState)

    suspend fun deleteFile(cachedFileState: CachedFileState)
}
