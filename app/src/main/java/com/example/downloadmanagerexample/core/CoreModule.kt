package com.example.downloadmanagerexample.core

import com.example.downloadmanagerexample.core.data.LocalFileDataSource
import com.example.downloadmanagerexample.core.data.RemoteFileDataSource
import com.example.downloadmanagerexample.core.data.android.AndroidFileDataSource
import com.example.downloadmanagerexample.core.data.android.AndroidRemoteFileDataSource
import com.example.downloadmanagerexample.core.utils.AppCoroutineScope
import com.example.downloadmanagerexample.core.utils.AppCoroutineScopeImpl
import com.example.downloadmanagerexample.core.utils.AppDispatchers
import com.example.downloadmanagerexample.core.utils.AppDispatchersImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single<AppCoroutineScope> { AppCoroutineScopeImpl() }
    single<AppDispatchers> { AppDispatchersImpl() }
    single<LocalFileDataSource> { AndroidFileDataSource(androidContext()) }
    single<RemoteFileDataSource> { AndroidRemoteFileDataSource(androidContext(), get()) }
}
