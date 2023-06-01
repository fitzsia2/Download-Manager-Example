package com.example.downloadmanagerexample.features.files.domain

import kotlinx.coroutines.flow.Flow

interface FileRepository {

    val remoteFileMetadataCacheStateStream: Flow<RemoteFileMetadataCacheState>

    fun downloadFile(remoteFileMetadata: RemoteFileMetadata)

    suspend fun deleteFile(cachedFileState: CachedFileState)
}
