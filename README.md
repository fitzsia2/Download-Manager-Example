# `DownloadManager` & Clean Architecture

Android offers a few options for downloading remote files. Each solution has
its own tradeoffs. Without getting into those tradeoffs, I'd like to offer
a more complete example of how you can use `DownloadManager` to, well,
manage your downloads.

## `DownloadManager`

`DownloadManager` offers a pretty simple API for downloading files. You can
create `DownloadManager.Request`, pointing to the URI of the remote file,
set the destination of where you would like the file to be downloaded and
some other options, such as the title and description of the notification
shown by the system (a nice feature of `DownloadManager`). Once enqueued,
the Android system takes over and manages the rest for you. The user is free
to close the app or switch to another app while the file is downloading. Progress
is also shown by a system notification.

To begin, I will assume that you already have an API which gives some basic
information about available downloads (name and URI). I will use a simple data
class for this information.

```kotlin
data class RemoteFileMetadata(val name: String, val uri: String)
```

From the high level, what does our app need to know about our download? We
need to know if the file is already downloaded, if it is being downloaded, or
if there was an error. We can create `CachedFileState` to model our domain
data:

```kotlin
sealed interface CachedFileState {

    val metadata: RemoteFileMetadata

    data class NotCached(
        override val metadata: RemoteFileMetadata
    ) : CachedFileState

    data class Cached(
        override val metadata: RemoteFileMetadata,
        val downloadedFile: DownloadedFile
    ) : CachedFileState

    data class Error(
        override val metadata: RemoteFileMetadata,
        val reason: DownloadError
    ) : CachedFileState

    data class Downloading(
        override val metadata: RemoteFileMetadata
    ) : CachedFileState
}
```

This is all the information that our domain layer needs to know about. If
your app needs to support extra states such as pending or paused, I would
suggest you add those as implementations of `CachedFileState`.

Our data layer will be responsible for creating `CachedFileState` based on the
information available from `DownloadManager`.

`DownloadManager` uses system services to manage the download even outside of
our own app. But what happens when the app is resumed? If your app was closed, we will not
have the id returned by the `DownloadManager.enqueue()` API. That means we need
to query `DownloadManager` to find our download. In most cases, the URI should
be unique so we can use that to find the status of our download. If your URIs are
not well known, you will need another value to manage its identity. That information
can be included to the `DownloadManager.Request` and queried later to match the data.
Here is where things get a little less convenient.

`DownloadManager's` query gives us a `Cursor`. `Cursors` have become less common
in modern Android development but for our case it is not too complicated. We simply need to use our URI to find
our download id and then translate data from the `Cursor` into our own. Since
other parts of our app do not need to know that we are using `DownloadManager`,
it is a good idea to create our own objects and protect our other components
from any dependencies on `DownloadManager`.

`DownloadManager` documentation describes 5 states: pending, running, paused, 
successful and error. I found it useful to create my own classes around these
states.

```kotlin
private sealed interface RemoteDownload {
    val id: Long
    val remoteUri: String

    data class Pending(...) : RemoteDownload
    data class Running(...) : RemoteDownload
    data class Paused(...) : RemoteDownload
    data class Successful(...) : RemoteDownload
    data class Failed(...) : RemoteDownload
}
```

Since `DownloadManager` will only return data related to our app, we can
query it without any filters and map each cursor to a `RemoteDownload`. Once we
have a collection of `RemoteDownloads` it is easy to map these to
`CachedFileStates` and provide them to our repository. Another thing to note is
that working with `Cursors` can block threads, so it's important to wrap these
operations with `withContext(Dispatchers.IO)`. It is a good practice to inject
these dispatchers where you need them instead of depending on
`Dispatchers.IO` directly.

Now, that we have an easy way to query `DownloadManager`, we need to design our
repository. In modern apps, `Flow` is king. So, let's expose a list of 
`CachedFileStates` through a `Flow` like:

```kotlin
interface FileRepository {
    ...
    fun getCachedFileStateStream(metadata: List<RemoteFileMetadata>): Flow<List<CachedFileState>>
}
```

Unfortunately, we're only able to poll our data source for the status, so we'll
need to implement it with a `Flow` builder and delay.

```kotlin
class FileRepositoryImpl : FileRepository {
    ...
    override fun getCachedFileStateStream(metadata: List<RemoteFileMetadata>): Flow<List<CachedFileState>> {
        return flow {
            while (true) {
                val cacheStates = metadata.map { getCacheState(it) }
                emit(cacheStates)
                delay(DOWNLOAD_STATE_POLL_PERIOD)
            }
        }
    }
}
```

This works fine, and we can even test it 🎉 You can find some example tests in
my repo.

We can optimize this further by sharing the returned flow. This way, we will not
have a process polling the download manager for each observer of the stream 💪
Since the flows returned here depend on metadata, we need to cache them. I’ve
wrapped the operation in `withContext()` to ensure access to our cache is thread safe.

```kotlin
class FileRepositoryImpl(
    private val appCoroutineScope: AppCoroutineScope,
    private val localFileDataSource: LocalFileDataSource,
    private val remoteFileDataSource: RemoteFileDataSource,
) : FileRepository {

    private val metadataCachedFileStateStreamMap = 
       mutableMapOf<List<RemoteFileMetadata>, Flow<List<CachedFileState>>>()

    override suspend fun getCachedFileStateStream(metadata: List<RemoteFileMetadata>): Flow<List<CachedFileState>> {
        return withContext(appDispatchers.main) {
            val cachedFileStateStream = metadataCachedFileStateStreamMap[metadata] ?: flow {
                while (true) {
                    val cacheStates = metadata.map { getCacheState(it) }
                    emit(cacheStates)
                    delay(DOWNLOAD_STATE_POLL_PERIOD)
                }
            }
                .distinctUntilChanged()
                .shareIn(
                    scope = appCoroutineScope,
                    started = SharingStarted.WhileSubscribed(),
                    replay = 1
                )
            metadataCachedFileStateStreamMap[metadata] = cachedFileStateStream
            cachedFileStateStream
        }
    }
}
```

To use `shareIn` we need a common coroutine context. Since our repository
should be a singleton, we can inject a coroutine scope created at the app
level or, while testing, from our tests.

## Architecture

This design tries to follow clean code and clean architecture practices. It is
important to avoid as much logic as possible in the data sources since they are
very difficult to test. Data sources usually depend on external libraries such
as, in our case `DownloadManager` or a remote API. Making them as simple as we
can allows us to create our own implementations that we can use for testing.
