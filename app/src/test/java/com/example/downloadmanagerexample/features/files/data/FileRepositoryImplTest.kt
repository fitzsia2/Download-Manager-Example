package com.example.downloadmanagerexample.features.files.data

import app.cash.turbine.test
import com.example.downloadmanagerexample.core.data.fake.FakeLocalFileDataSource
import com.example.downloadmanagerexample.core.data.fake.FakeRemoteFileDataSource
import com.example.downloadmanagerexample.core.utils.CoroutineTest
import com.example.downloadmanagerexample.features.files.domain.CachedFileState
import com.example.downloadmanagerexample.features.files.domain.DownloadedFile
import com.example.downloadmanagerexample.features.files.domain.FileRepository
import com.example.downloadmanagerexample.features.files.domain.RemoteFileMetadata
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
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
    fun `State stream emits Downloading when file is downloading`() = runTest {
        val metadata = listOf(RemoteFileMetadata("", ""))
        fakeRemoteFileDataSource.cachedFileStates = metadata.map { CachedFileState.Downloading(it) }

        val actual = getUnderTest().getCachedFileStateStream(metadata).first()

        assertThat(actual.first()).isInstanceOf(CachedFileState.Downloading::class.java)
    }

    @Test
    fun `State stream emits Cached when file is cached`() = runTest {
        val remoteFileMetadata = RemoteFileMetadata("", "")
        val metadata = listOf(remoteFileMetadata)
        fakeRemoteFileDataSource.cachedFileStates = metadata.mapToCached()

        val actual = getUnderTest().getCachedFileStateStream(metadata).first()

        assertThat(actual.first()).isInstanceOf(CachedFileState.Cached::class.java)
    }

    private fun List<RemoteFileMetadata>.mapToCached() = map { remoteFileMetadata ->
        val file = DownloadedFile(remoteFileMetadata.name, fakeLocalFileDataSource.getDownloadedFile(remoteFileMetadata.name))
        CachedFileState.Cached(remoteFileMetadata, file)
    }

    @Test
    fun `State stream emits NotCached when file is not cached`() = runTest {
        val remoteFileMetadata = RemoteFileMetadata("", "")
        val metadata = listOf(remoteFileMetadata)

        val actual = getUnderTest().getCachedFileStateStream(metadata).first()

        assertThat(actual.first()).isInstanceOf(CachedFileState.NotCached::class.java)
    }

    @Test
    fun `State stream emits Cached when download finishes`() = runTest {
        val metadata = listOf(RemoteFileMetadata("", ""))
        fakeRemoteFileDataSource.cachedFileStates = metadata.map { CachedFileState.Downloading(it) }

        getUnderTest().getCachedFileStateStream(metadata).test {
            assertThat(awaitItem().first()).isInstanceOf(CachedFileState.Downloading::class.java)

            fakeRemoteFileDataSource.cachedFileStates = metadata.mapToCached()
            assertThat(awaitItem().first()).isInstanceOf(CachedFileState.Cached::class.java)
        }
    }

    companion object {

        @TempDir
        lateinit var tempFolder: Path
    }
}
