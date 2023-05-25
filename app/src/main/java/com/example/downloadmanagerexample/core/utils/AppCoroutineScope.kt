package com.example.downloadmanagerexample.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Coroutine scope that is bound to application lifecycle
 */
interface AppCoroutineScope : CoroutineScope

class AppCoroutineScopeImpl : AppCoroutineScope {

    override val coroutineContext = SupervisorJob()
}
