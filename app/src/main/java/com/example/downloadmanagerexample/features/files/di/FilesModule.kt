package com.example.downloadmanagerexample.features.files.di

import com.example.downloadmanagerexample.features.files.data.FileRepositoryImpl
import com.example.downloadmanagerexample.features.files.domain.FileRepository
import com.example.downloadmanagerexample.features.files.presentation.manager.FileManagerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val filesModule = module {
    single<FileRepository> { FileRepositoryImpl(get(), get()) }
    viewModel { FileManagerViewModel(get()) }
}
