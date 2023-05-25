package com.example.downloadmanagerexample.features.photos.di

import com.example.downloadmanagerexample.features.photos.data.PhotoRepositoryImpl
import com.example.downloadmanagerexample.features.photos.domain.PhotoRepository
import com.example.downloadmanagerexample.features.photos.presentation.manager.PhotoManagerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val photosModule = module {
    single<PhotoRepository> { PhotoRepositoryImpl(get(), get(), get()) }
    viewModel { PhotoManagerViewModel(get()) }
}
