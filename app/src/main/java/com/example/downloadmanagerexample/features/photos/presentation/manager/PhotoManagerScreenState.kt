package com.example.downloadmanagerexample.features.photos.presentation.manager

import com.example.downloadmanagerexample.features.photos.domain.PhotoCacheState

sealed interface PhotoManagerScreenState {

    object Loading : PhotoManagerScreenState

    object Error : PhotoManagerScreenState

    data class Loaded(val photoCacheState: List<PhotoCacheState>) : PhotoManagerScreenState
}
