package com.example.downloadmanagerexample.core.domain

sealed interface DownloadError {

    object Unknown : DownloadError

    object FileDownloadError : DownloadError

    object UnhandledHttpCode : DownloadError

    object HttpData : DownloadError

    object TooManyRedirects : DownloadError

    object InsufficientSpace : DownloadError

    object DeviceNotFound : DownloadError

    object CannotResume : DownloadError

    object FileAlreadyExists : DownloadError

    data class Http(val code: Int) : DownloadError
}
