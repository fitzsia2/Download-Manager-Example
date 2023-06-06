package com.example.downloadmanagerexample.features.files.presentation.manager

import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadataCacheState

sealed interface FileManagerScreenState {

    object Loading : FileManagerScreenState

    object Error : FileManagerScreenState

    data class Loaded(val cachedFileState: List<CachedFileState>) : FileManagerScreenState

    companion object {

        operator fun invoke(cacheState: RemoteFileMetadataCacheState): FileManagerScreenState {
            return when (cacheState) {
                is RemoteFileMetadataCacheState.Error -> Error
                is RemoteFileMetadataCacheState.Synchronized -> Loaded(cacheState.cachedFileStates)
                is RemoteFileMetadataCacheState.Synchronizing -> Loading
            }
        }
    }
}
