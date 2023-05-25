package com.example.downloadmanagerexample.core.data

import java.io.File

interface LocalFileDataSource {

    /**
     * Gets [File] from the downloaded files directory. The file may or may not already exist.
     */
    fun getDownloadedFile(fileName: String): File

    fun getDownloadedFiles(): List<File>
}
