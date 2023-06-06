package com.example.downloadmanagerexample.features.files.presentation.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FileManagerViewModel(
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _screenStateStream = MutableStateFlow<FileManagerScreenState>(FileManagerScreenState.Loading)
    val screenStateStream: StateFlow<FileManagerScreenState> = _screenStateStream

    init {
        fileRepository.remoteFileMetadataCacheStateStream
            .map(FileManagerScreenState::invoke)
            .onEach { _screenStateStream.value = it }
            .launchIn(viewModelScope)
    }

    fun download(cachedFileState: CachedFileState) {
        fileRepository.downloadFile(cachedFileState)
    }

    fun delete(cachedFileState: CachedFileState) {
        viewModelScope.launch { fileRepository.deleteFile(cachedFileState) }
    }
}
