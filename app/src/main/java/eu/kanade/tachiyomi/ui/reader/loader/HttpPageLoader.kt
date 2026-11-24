package eu.kanade.tachiyomi.ui.reader.loader

import app.luma.data.cache.ChapterCache
import eu.kanade.domain.chapter.model.toSChapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Loader used to load chapters from an online source.
 */
internal class HttpPageLoader(
    private val chapter: ReaderChapter,
    private val source: HttpSource,
    private val chapterCache: ChapterCache,
    private val appScope: CoroutineScope,
    private val canLoadAhead: () -> Boolean,
) : PageLoader() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Guards [chain], [chainPosition] and the pump that starts downloads from them. */
    private val chainLock = Any()

    /** Pages the chain still has to start, nearest to the page being read first. */
    private var chain: List<ReaderPage> = emptyList()

    /** How far [chain] has been walked. */
    private var chainPosition = 0

    private val downloadJobs = ConcurrentHashMap<Int, Job>()

    override var isLocal: Boolean = false

    /**
     * Returns the page list for a chapter. It tries to return the page list from the local cache,
     * otherwise fallbacks to network.
     */
    override suspend fun getPages(): List<ReaderPage> {
        val pages = chapterCache.getPageList(chapter.chapter)
            ?: source.getPageList(chapter.chapter.toSChapter())
        return pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderPage(index, page.url, page.imageUrl).also {
                // Loading starts below, before ChapterLoader gets to set this, and chapter is a lateinit
                it.chapter = chapter
            }
        }
            .also {
                it.startChain(0)
            }
    }

    /**
     * Loads a page through the queue. Handles re-enqueueing pages if they were evicted from the cache.
     */
    override suspend fun loadPage(page: ReaderPage) = withContext(Dispatchers.IO) {
        val imageUrl = page.imageUrl

        // Check if the image has been deleted
        if (page.status == Page.State.Ready && imageUrl != null && !chapterCache.isImageInCache(imageUrl)) {
            page.status = Page.State.Queue
        }

        // Automatically retry failed pages when subscribed to this page
        if (page.status is Page.State.Error) {
            page.status = Page.State.Queue
        }

        // Start the page being read before anything else. Planning the chain walks the whole chapter and takes
        // chainLock, and the chain only reaches this page once it has, so don't make it wait on either.
        launchLoadOnce(page)

        val pages = page.chapter.pages.orEmpty()
        if (page.index !in pages.indices) return@withContext
        pages.startChain(page.index)
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: ReaderPage) {
        if (page.status is Page.State.Error) {
            page.status = Page.State.Queue
        }

        val pageIndex = page.index
        downloadJobs.remove(pageIndex)?.cancel()
        scope.launch { internalLoadPage(page, force = true) }.trackAsDownloadJob(pageIndex)
    }

    override fun recycle() {
        super.recycle()
        scope.cancel()
        downloadJobs.clear()
        synchronized(chainLock) {
            chain = emptyList()
            chainPosition = 0
        }

        // Cache current page list progress for online chapters to allow a faster reopen
        chapter.pages?.let { pages ->
            appScope.launch(Dispatchers.IO) {
                try {
                    // Convert to pages without reader information
                    val pagesToSave = pages.map { Page(it.index, it.url, it.imageUrl) }
                    chapterCache.putPageList(chapter.chapter, pagesToSave)
                } catch (e: Throwable) {
                    if (e is CancellationException) {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Keeps [this] in [downloadJobs] under [pageIndex] until it completes. The entry is only removed if it still
     * points at this job, so a newer job for the same page isn't evicted by an older one finishing.
     */
    private fun Job.trackAsDownloadJob(pageIndex: Int) = also { job ->
        downloadJobs[pageIndex] = job
        job.invokeOnCompletion { onDownloadFinished(pageIndex, job) }
    }

    /**
     * Frees the page's slot and lets the chain start whatever is next. Downloads end on their own threads, so the
     * slot and the pump are taken under the lock the chain is read under.
     */
    private fun onDownloadFinished(pageIndex: Int, job: Job) {
        downloadJobs.remove(pageIndex, job)
        synchronized(chainLock) { pumpLocked() }
    }

    /**
     * Starts loading [page] unless it's already loaded or a load is already in flight for it. Claiming the slot and
     * starting the job has to be atomic, otherwise two chains can both launch the same page.
     */
    private fun launchLoadOnce(page: ReaderPage): Job? {
        val pageIndex = page.index
        // A page another chain is already loading still counts towards this chain, otherwise the chain moves on
        // and starts the next page while it's in flight
        downloadJobs[pageIndex]?.takeIf { it.isActive }?.let { return it }
        if (page.status !is Page.State.Queue && page.status !is Page.State.Error) return null

        val job = scope.launch(start = CoroutineStart.LAZY) { internalLoadPage(page) }
        val running = downloadJobs.putIfAbsent(pageIndex, job)
        if (running != null) {
            job.cancel()
            // Hand back the in flight job so the chain still waits for this page and the concurrency bound keeps
            // holding how many downloads run at once
            return running
        }
        job.invokeOnCompletion { onDownloadFinished(pageIndex, job) }
        job.start()
        return job
    }

    /**
     * Replaces the chain with the pages around [index] and starts as many of them as the concurrency bound allows.
     */
    private fun List<ReaderPage>.startChain(index: Int) {
        val ordered = nearestFirstOrder(index)
        synchronized(chainLock) {
            chain = ordered
            chainPosition = 0
            cancelDistantDownloadsLocked(index)
            pumpLocked()
        }
    }

    /**
     * The pages to fetch around [index], ordered outwards from it: the page being read, then the one after it and
     * the one before it, then the next pair out, and so on.
     *
     * Working outwards is what keeps a page the reader has just scrolled past from waiting for the rest of the
     * chapter. Taking everything ahead of the reader first meant anything behind was fetched last, or cancelled by
     * the next page turn. When loading ahead isn't allowed the chain is only the pages near the one being read.
     */
    private fun List<ReaderPage>.nearestFirstOrder(index: Int): List<ReaderPage> {
        if (isEmpty() || index !in indices) return emptyList()
        val first = if (canLoadAhead()) 0 else (index - 1).coerceAtLeast(0)
        val last = if (canLoadAhead()) lastIndex else (index + LIMITED_PAGES_AHEAD).coerceAtMost(lastIndex)
        if (first > last) return emptyList()

        val ordered = ArrayList<ReaderPage>(last - first + 1)
        fun addAt(position: Int) {
            if (position in first..last) ordered.add(this[position])
        }
        addAt(index)
        var distance = 1
        val count = last - first + 1
        while (ordered.size < count) {
            addAt(index + distance)
            addAt(index - distance)
            distance++
        }
        return ordered
    }

    /**
     * Starts chain pages while there's room. A finished download fills its slot right away instead of waiting for
     * the rest of a batch, so one slow page no longer holds up everything queued behind it.
     */
    private fun pumpLocked() {
        while (downloadJobs.size < MAX_CONCURRENT_DOWNLOADS) {
            val page = chain.getOrNull(chainPosition) ?: return
            chainPosition++
            launchLoadOnce(page)
        }
    }

    /**
     * Drops downloads the reader has moved away from. Pages near the one being read keep running, so turning pages
     * quickly no longer restarts work the reader is about to need.
     */
    private fun cancelDistantDownloadsLocked(index: Int) {
        downloadJobs.keys
            .filter { abs(it - index) > KEEP_IN_FLIGHT_DISTANCE }
            .forEach { downloadJobs.remove(it)?.cancel() }
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary.
     * Downloaded images are stored in the chapter cache.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private suspend fun internalLoadPage(page: ReaderPage, force: Boolean = false) {
        try {
            if (page.imageUrl.isNullOrEmpty()) {
                page.status = Page.State.LoadPage
                page.imageUrl = source.getImageUrl(page)
            }
            val imageUrl = page.imageUrl!!

            val file = chapterCache.getImageFile(imageUrl).takeUnless { force } ?: run {
                val live = page.downloadStream
                page.status = Page.State.DownloadImage
                try {
                    chapterCache.putImage(imageUrl, source.getImage(page), live)
                        .also { live?.finish() }
                } catch (e: Throwable) {
                    live?.finish(e)
                    throw e
                }
            }

            page.downloadStream = null
            page.stream = {
                try {
                    file.inputStream()
                } catch (e: FileNotFoundException) {
                    // Gone from the cache since, so it's fetched again and shown once it's back
                    page.status = Page.State.Queue
                    launchLoadOnce(page)
                    throw e
                }
            }
            page.status = Page.State.Ready
        } catch (e: Throwable) {
            page.downloadStream = null
            if (e is CancellationException) {
                // Chains get replaced on every page turn, so a cancelled page isn't an error. Put it back in
                // the queue, otherwise it's stuck in a state no later pass will pick up.
                page.status = Page.State.Queue
                throw e
            }
            page.status = Page.State.Error(e)
        }
    }
}

private const val LIMITED_PAGES_AHEAD = 4

/**
 * Downloads allowed at once. Six keeps up with a normal scroll even from a source that needs about two seconds per
 * image, and the pages being read come first, so the extra slots only fill in speculation.
 */
private const val MAX_CONCURRENT_DOWNLOADS = 6

/** How far from the page being read a download survives a page turn. */
private const val KEEP_IN_FLIGHT_DISTANCE = 8
