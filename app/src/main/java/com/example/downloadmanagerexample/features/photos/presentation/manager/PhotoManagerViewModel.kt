package com.example.downloadmanagerexample.features.photos.presentation.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.downloadmanagerexample.features.photos.domain.PhotoCacheState
import com.example.downloadmanagerexample.features.photos.domain.PhotoRepository
import com.example.downloadmanagerexample.features.photos.domain.RemotePhotoMetadataCacheState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class PhotoManagerViewModel(
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val _screenStateStream = MutableStateFlow<PhotoManagerScreenState>(PhotoManagerScreenState.Loading)
    val screenStateStream: StateFlow<PhotoManagerScreenState> = _screenStateStream

    init {
        photoRepository.remotePhotoMetadataCacheStateStream
            .map {
                when (it) {
                    is RemotePhotoMetadataCacheState.Error -> PhotoManagerScreenState.Error
                    is RemotePhotoMetadataCacheState.Synchronized -> PhotoManagerScreenState.Loaded(it.photoCacheStates)
                    is RemotePhotoMetadataCacheState.Synchronizing -> PhotoManagerScreenState.Loading
                }
            }
            .onEach { _screenStateStream.value = it }
            .launchIn(viewModelScope)
    }

    fun downloadPhoto(photoCacheState: PhotoCacheState) {
        photoRepository.downloadPhoto(photoCacheState.metadata)
    }
}
