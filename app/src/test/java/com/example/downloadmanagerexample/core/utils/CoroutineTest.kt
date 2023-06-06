package com.example.downloadmanagerexample.core.utils

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.extension.RegisterExtension

@Suppress("UnnecessaryAbstractClass")
internal abstract class CoroutineTest(testDispatcher: TestDispatcher = StandardTestDispatcher()) {

    @field:RegisterExtension
    val coroutinesExtension = CoroutinesExtension(testDispatcher)

    var testAppCoroutineScope = TestAppCoroutineScope(testDispatcher)
        private set
}
