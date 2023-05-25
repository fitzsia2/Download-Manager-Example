package com.example.downloadmanagerexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.downloadmanagerexample.core.theme.DownloadManagerExampleTheme
import com.example.downloadmanagerexample.features.photos.presentation.manager.PhotoManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DownloadManagerExampleTheme {
                PhotoManager(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
