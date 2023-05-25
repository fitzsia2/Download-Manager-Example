package com.example.downloadmanagerexample.features.photos.presentation.manager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.downloadmanagerexample.features.photos.domain.PhotoCacheState
import org.koin.androidx.compose.getViewModel

@Composable
fun PhotoManager(modifier: Modifier = Modifier, viewModel: PhotoManagerViewModel = getViewModel()) {
    val screenState = viewModel.screenStateStream.collectAsState().value
    Scaffold(modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            when (screenState) {
                is PhotoManagerScreenState.Error -> TODO()
                is PhotoManagerScreenState.Loaded -> Loaded(screenState = screenState, downloadPhoto = viewModel::downloadPhoto)
                is PhotoManagerScreenState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun Loaded(screenState: PhotoManagerScreenState.Loaded, downloadPhoto: (PhotoCacheState.NotCached) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(screenState.photoCacheState.size) { index ->
            PhotoCacheStateItem(screenState.photoCacheState[index], downloadPhoto)
        }
    }
}

@Composable
private fun PhotoCacheStateItem(photoCacheState: PhotoCacheState, downloadPhoto: (PhotoCacheState.NotCached) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                text = photoCacheState.metadata.name
            )
            PhotoCacheState(photoCacheState, downloadPhoto = downloadPhoto, modifier = Modifier.align(Alignment.CenterVertically))
        }
        AnimatedVisibility(visible = photoCacheState is PhotoCacheState.Cached) {
            if (photoCacheState is PhotoCacheState.Cached) {
                AsyncImage(
                    modifier = Modifier.fillMaxWidth(),
                    model = photoCacheState.photo.file.toUri(),
                    contentDescription = photoCacheState.metadata.name
                )
            }
        }
    }
}

@Composable
private fun PhotoCacheState(photoCacheState: PhotoCacheState, downloadPhoto: (PhotoCacheState.NotCached) -> Unit, modifier: Modifier = Modifier) {
    val text = when (photoCacheState) {
        is PhotoCacheState.Cached -> "Delete"
        is PhotoCacheState.Error -> "Error"
        is PhotoCacheState.Downloading -> "Downloading"
        is PhotoCacheState.NotCached -> "Download"
    }
    Button(
        modifier = modifier,
        onClick = {
            if (photoCacheState is PhotoCacheState.NotCached) {
                downloadPhoto(photoCacheState)
            }
        },
        enabled = photoCacheState is PhotoCacheState.NotCached
    ) {
        AnimatedVisibility(visible = photoCacheState is PhotoCacheState.Downloading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp)
            )
        }
        Text(text = text)
    }
}
