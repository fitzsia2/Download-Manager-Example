package com.example.downloadmanagerexample.core.data.android

import android.content.Context
import android.os.Environment
import com.example.downloadmanagerexample.core.data.LocalFileDataSource
import java.io.File

class AndroidFileDataSource(
    context: Context,
) : LocalFileDataSource {

    private val downloadDirectory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)

    override fun getDownloadedFile(fileName: String): File {
        val file = File(downloadDirectory, fileName)
        if (file.doesNotExist()) {
            file.parentFile?.mkdirs()
        }
        return file
    }

    private fun File.doesNotExist(): Boolean = !exists()

    override fun getDownloadedFiles(): List<File> {
        return downloadDirectory?.listFiles()?.toList() ?: emptyList()
    }
}
