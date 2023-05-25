package com.example.downloadmanagerexample.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatchers for Coroutines that can be replaced in tests
 */
interface AppDispatchers {

    val io: CoroutineDispatcher

    val main: CoroutineDispatcher
}

class AppDispatchersImpl : AppDispatchers {

    override val io: CoroutineDispatcher = Dispatchers.IO

    override val main: CoroutineDispatcher = Dispatchers.Main
}
