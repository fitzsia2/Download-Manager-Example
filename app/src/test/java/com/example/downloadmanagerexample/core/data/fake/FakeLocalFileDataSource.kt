package com.example.downloadmanagerexample.core.data.fake

import com.example.downloadmanagerexample.core.data.LocalFileDataSource
import java.io.File
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

internal class FakeLocalFileDataSource(private val tempFolder: Path) : LocalFileDataSource {

    override fun getDownloadedFile(fileName: String): File {
        return tempFolder.resolve(fileName).toFile()
    }

    override fun getDownloadedFiles(): List<File> {
        return tempFolder.listDirectoryEntries().map { it.toFile() }
    }

    override fun deleteFile(file: File) {
        throw NotImplementedError()
    }
}
