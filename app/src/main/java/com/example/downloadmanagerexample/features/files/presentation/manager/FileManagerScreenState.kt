package com.example.downloadmanagerexample.features.files.presentation.manager

import com.example.downloadmanagerexample.features.files.domain.CachedFileState

sealed interface FileManagerScreenState {

    object Loading : FileManagerScreenState

    object Error : FileManagerScreenState

    data class Loaded(val cachedFileState: List<CachedFileState>) : FileManagerScreenState
}
