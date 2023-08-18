package com.example.downloadmanagerexample.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal object TestAppDispatchers : AppDispatchers {

    override val io: CoroutineDispatcher = Dispatchers.Unconfined

    override val main: CoroutineDispatcher = Dispatchers.Unconfined
}
