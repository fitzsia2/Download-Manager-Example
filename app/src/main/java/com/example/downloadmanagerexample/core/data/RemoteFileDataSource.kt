package com.example.downloadmanagerexample.core.data

import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadata
import java.io.File

interface RemoteFileDataSource {

    suspend fun getRemoteFileMetadata(): List<RemoteFileMetadata>

    fun downloadToFile(uri: String, file: File)

    suspend fun synchronize(metadata: List<RemoteFileMetadata>)

    suspend fun getCachedFileState(metadata: RemoteFileMetadata): CachedFileState

    suspend fun removeDownload(remoteUri: String)
}
