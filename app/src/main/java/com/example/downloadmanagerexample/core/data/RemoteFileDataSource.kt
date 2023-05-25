package com.example.downloadmanagerexample.core.data

import android.net.Uri
import java.io.File

interface RemoteFileDataSource {

    fun downloadToFile(uri: Uri, file: File)

    suspend fun isDownloadInProgress(uri: Uri, file: File): Boolean

    suspend fun removeDownload(uri: Uri)
}
