package com.example.downloadmanagerexample.features.files.presentation.manager

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
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import org.koin.androidx.compose.getViewModel

interface FileActions {

    fun download(cachedFileState: CachedFileState)

    fun delete(cachedFileState: CachedFileState)
}

@Composable
fun FileManager(modifier: Modifier = Modifier, viewModel: FileManagerViewModel = getViewModel()) {
    val screenState = viewModel.screenStateStream.collectAsState().value
    val actions = remember {
        object : FileActions {
            override fun download(cachedFileState: CachedFileState) {
                viewModel.download(cachedFileState)
            }

            override fun delete(cachedFileState: CachedFileState) {
                viewModel.delete(cachedFileState)
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
                is FileManagerScreenState.Error -> TODO()
                is FileManagerScreenState.Loaded -> Loaded(screenState = screenState, actions)
                is FileManagerScreenState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun Loaded(screenState: FileManagerScreenState.Loaded, actions: FileActions, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(screenState.cachedFileState.size) { index ->
            FileCacheStateItem(screenState.cachedFileState[index], actions)
        }
    }
}

@Composable
private fun FileCacheStateItem(cachedFileState: CachedFileState, actions: FileActions) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                text = cachedFileState.metadata.name
            )
            CachedFileState(cachedFileState, actions = actions, modifier = Modifier.align(Alignment.CenterVertically))
        }
        AnimatedVisibility(visible = cachedFileState is CachedFileState.Cached) {
            if (cachedFileState is CachedFileState.Cached) {
                AsyncImage(
                    modifier = Modifier.fillMaxWidth(),
                    model = cachedFileState.file.file.toUri(),
                    contentDescription = cachedFileState.metadata.name
                )
            }
        }
    }
}

@Composable
private fun CachedFileState(cachedFileState: CachedFileState, actions: FileActions, modifier: Modifier = Modifier) {
    val text = when (cachedFileState) {
        is CachedFileState.Cached -> "Delete"
        is CachedFileState.Error -> "Error"
        is CachedFileState.Downloading -> "Downloading"
        is CachedFileState.NotCached -> "Download"
    }
    Button(
        modifier = modifier,
        onClick = {
            when (cachedFileState) {
                is CachedFileState.NotCached -> actions.download(cachedFileState)
                is CachedFileState.Cached -> actions.delete(cachedFileState)
                is CachedFileState.Downloading -> actions.delete(cachedFileState)
                else -> Unit
            }
        },
        enabled = cachedFileState !is CachedFileState.Error
    ) {
        AnimatedVisibility(visible = cachedFileState is CachedFileState.Downloading) {
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
