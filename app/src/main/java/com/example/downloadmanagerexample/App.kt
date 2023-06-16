package com.example.downloadmanagerexample

import android.app.Application
import com.example.downloadmanagerexample.core.coreModule
import com.example.downloadmanagerexample.features.files.di.filesModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin()
        initTimber()
    }

    private fun startKoin() {
        startKoin {
            androidContext(this@App)
            modules(coreModule, filesModule)
        }
    }

    private fun initTimber() {
        Timber.plant(Timber.DebugTree())
    }
}
