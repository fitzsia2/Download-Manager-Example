package com.example.downloadmanagerexample.features.files.data

import com.example.downloadmanagerexample.core.data.fake.FakeLocalFileDataSource
import com.example.downloadmanagerexample.core.data.fake.FakeRemoteFileDataSource
import com.example.downloadmanagerexample.core.utils.CoroutineTest
import com.example.downloadmanagerexample.features.files.domain.FileRepository
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadataCacheState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

internal class FileRepositoryImplTest : CoroutineTest() {

    private val fakeLocalFileDataSource = FakeLocalFileDataSource(tempFolder)

    private val fakeRemoteFileDataSource = FakeRemoteFileDataSource()

    @AfterEach
    fun cleanUp() {
        tempFolder.listDirectoryEntries().forEach { it.deleteIfExists() }
    }

    private fun getUnderTest(): FileRepository = FileRepositoryImpl(testAppCoroutineScope, fakeLocalFileDataSource, fakeRemoteFileDataSource)

    @Test
    fun `Emits synchronizing`() = runTest {
        val actual = getUnderTest().remoteFileMetadataCacheStateStream.first()

        assertThat(actual).isInstanceOf(RemoteFileMetadataCacheState.Synchronizing::class.java)
    }

    @Test
    fun `Emits Synchronized after init`() = runTest {
        val underTest = getUnderTest()
        advanceUntilIdle()

        val actual = underTest.remoteFileMetadataCacheStateStream.first()

        assertThat(actual).isInstanceOf(RemoteFileMetadataCacheState.Synchronized::class.java)
    }

    companion object {

        @TempDir
        lateinit var tempFolder: Path
    }
}
