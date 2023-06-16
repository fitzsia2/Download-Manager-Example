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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.downloadmanagerexample.R
import com.example.downloadmanagerexample.core.domain.DownloadError
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import org.koin.androidx.compose.getViewModel
import timber.log.Timber

interface FileActions {

    fun download(cachedFileState: CachedFileState)

    fun delete(cachedFileState: CachedFileState)
}

@Composable
fun FileManagerScreen(modifier: Modifier = Modifier, viewModel: FileManagerViewModel = getViewModel()) {
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
                is FileManagerScreenState.Error -> TODO() // Create full screen error state
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
        if (cachedFileState is CachedFileState.Error) {
            ErrorMessage(cachedFileState.reason)
        }
        AnimatedVisibility(visible = cachedFileState is CachedFileState.Cached) {
            if (cachedFileState is CachedFileState.Cached) {
                AsyncImage(
                    modifier = Modifier.fillMaxWidth(),
                    model = cachedFileState.downloadedFile.file.toUri(),
                    contentDescription = cachedFileState.metadata.name
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(reason: DownloadError) {
    val res = when (reason) {
        is DownloadError.CannotResume -> R.string.download_error_cannot_resume
        is DownloadError.FileAlreadyExists -> R.string.download_error_file_already_exists
        is DownloadError.InsufficientSpace -> R.string.download_error_insufficient_space
        is DownloadError.FileDownloadError,
        is DownloadError.DeviceNotFound,
        is DownloadError.Http,
        is DownloadError.HttpData,
        is DownloadError.TooManyRedirects,
        is DownloadError.UnhandledHttpCode,
        is DownloadError.Unknown -> {
            Timber.w("Download error: $reason")
            R.string.download_error_general
        }
    }
    val text = stringResource(res)
    val formattedMessage = stringResource(R.string.download_error_message, text)
    Text(text = formattedMessage, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun CachedFileState(cachedFileState: CachedFileState, actions: FileActions, modifier: Modifier = Modifier) {
    val text = when (cachedFileState) {
        is CachedFileState.Cached -> stringResource(R.string.cached_file_state_button_delete)
        is CachedFileState.Error -> stringResource(R.string.cached_file_state_button_retry)
        is CachedFileState.Downloading -> stringResource(R.string.cached_file_state_button_downloading)
        is CachedFileState.NotCached -> stringResource(R.string.cached_file_state_button_download)
    }
    val buttonColor = when (cachedFileState) {
        is CachedFileState.Cached -> MaterialTheme.colorScheme.secondary
        is CachedFileState.Downloading -> MaterialTheme.colorScheme.tertiary
        is CachedFileState.Error -> MaterialTheme.colorScheme.error
        is CachedFileState.NotCached -> MaterialTheme.colorScheme.primary
    }
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        onClick = {
            when (cachedFileState) {
                is CachedFileState.NotCached -> actions.download(cachedFileState)
                is CachedFileState.Cached -> actions.delete(cachedFileState)
                is CachedFileState.Downloading -> actions.delete(cachedFileState)
                is CachedFileState.Error -> actions.download(cachedFileState)
            }
        },
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
