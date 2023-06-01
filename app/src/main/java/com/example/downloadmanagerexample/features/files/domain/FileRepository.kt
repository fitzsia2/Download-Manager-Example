package com.example.downloadmanagerexample.features.files.domain

import kotlinx.coroutines.flow.Flow

interface FileRepository {

    val remoteFileMetadataCacheStateStream: Flow<RemoteFileMetadataCacheState>

    fun downloadFile(cachedFileState: CachedFileState)

    suspend fun deleteFile(cachedFileState: CachedFileState)
}
