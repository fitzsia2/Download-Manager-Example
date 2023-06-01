package com.example.downloadmanagerexample.core.data

import android.net.Uri
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadata
import java.io.File

interface RemoteFileDataSource {

    fun downloadToFile(uri: Uri, file: File)

    suspend fun synchronize(metadata: List<RemoteFileMetadata>): List<CachedFileState>

    suspend fun getCachedFileState(metadata: RemoteFileMetadata): CachedFileState

    suspend fun removeDownload(remoteUri: Uri)
}
