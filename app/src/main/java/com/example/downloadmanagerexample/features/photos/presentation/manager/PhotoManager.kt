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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.downloadmanagerexample.features.photos.domain.PhotoCacheState
import org.koin.androidx.compose.getViewModel

interface PhotoActions {

    fun downloadPhoto(photoCacheState: PhotoCacheState)

    fun deletePhoto(photoCacheState: PhotoCacheState)
}

@Composable
fun PhotoManager(modifier: Modifier = Modifier, viewModel: PhotoManagerViewModel = getViewModel()) {
    val screenState = viewModel.screenStateStream.collectAsState().value
    val actions = remember {
        object : PhotoActions {
            override fun downloadPhoto(photoCacheState: PhotoCacheState) {
                viewModel.downloadPhoto(photoCacheState)
            }

            override fun deletePhoto(photoCacheState: PhotoCacheState) {
                viewModel.deletePhoto(photoCacheState)
            }
        }
    }
    Scaffold(modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            when (screenState) {
                is PhotoManagerScreenState.Error -> TODO()
                is PhotoManagerScreenState.Loaded -> Loaded(screenState = screenState, actions)
                is PhotoManagerScreenState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun Loaded(screenState: PhotoManagerScreenState.Loaded, actions: PhotoActions, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(screenState.photoCacheState.size) { index ->
            PhotoCacheStateItem(screenState.photoCacheState[index], actions)
        }
    }
}

@Composable
private fun PhotoCacheStateItem(photoCacheState: PhotoCacheState, actions: PhotoActions) {
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
            PhotoCacheState(photoCacheState, actions = actions, modifier = Modifier.align(Alignment.CenterVertically))
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
private fun PhotoCacheState(photoCacheState: PhotoCacheState, actions: PhotoActions, modifier: Modifier = Modifier) {
    val text = when (photoCacheState) {
        is PhotoCacheState.Cached -> "Delete"
        is PhotoCacheState.Error -> "Error"
        is PhotoCacheState.Downloading -> "Downloading"
        is PhotoCacheState.NotCached -> "Download"
    }
    Button(
        modifier = modifier,
        onClick = {
            when (photoCacheState) {
                is PhotoCacheState.NotCached -> actions.downloadPhoto(photoCacheState)
                is PhotoCacheState.Cached -> actions.deletePhoto(photoCacheState)
                is PhotoCacheState.Downloading -> actions.deletePhoto(photoCacheState)
                else -> Unit
            }
        },
        enabled = photoCacheState !is PhotoCacheState.Error
    ) {
        AnimatedVisibility(visible = photoCacheState is PhotoCacheState.Downloading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(text = text)
    }
}
