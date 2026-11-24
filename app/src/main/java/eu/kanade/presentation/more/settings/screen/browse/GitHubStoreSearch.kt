package eu.kanade.presentation.more.settings.screen.browse

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import kotlin.coroutines.cancellation.CancellationException

/**
 * Finds extension stores on GitHub and tells whether each one is actually installable.
 *
 * There is deliberately no built-in list of recommended stores: a store can execute code and read
 * the accounts signed in through its sources, so naming one is an endorsement nobody here is in a
 * position to make. Searching instead means the user picks a repository by its own description and
 * star count, and this class only rules out the ones that do not work.
 */
@Inject
@SingleIn(AppScope::class)
class GitHubStoreSearch(
    private val network: NetworkHelper,
    private val json: Json,
) {

    /**
     * One search, then verify each hit in parallel.
     *
     * [onProgress] reports how many candidates have been checked so far, so the screen can show
     * that work is happening rather than a spinner that lasts as long as the slowest probe.
     */
    suspend fun search(
        query: String,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> },
    ): List<StoreCandidate> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val found = searchRepositories(trimmed)
        if (found.isEmpty()) return@withContext emptyList()

        var done = 0
        val verified = coroutineScope {
            found.map { repo ->
                async {
                    val result = verify(repo)
                    synchronized(this@coroutineScope) {
                        done++
                        onProgress(done, found.size)
                    }
                    result
                }
            }.awaitAll().filterNotNull()
        }
        // Most stars first: for a fork with no reputation to go on, that is the only signal.
        verified.sortedByDescending { it.stars }
    }

    private suspend fun searchRepositories(query: String): List<Repo> {
        // GitHub bills a search by request, not by result, so a full page costs the same as a
        // partial one.
        val url = "https://api.github.com/search/repositories" +
            "?q=${query.encodeUrl()}&sort=stars&order=desc&per_page=30"
        return try {
            val body = network.client.newCall(
                GET(
                    url,
                    headers = Headers.Builder()
                        .add("Accept", "application/vnd.github+json")
                        .add("User-Agent", "Tachiyomi")
                        .build(),
                ),
            ).awaitSuccess().body.string()

            val root = json.parseToJsonElement(body).jsonObject
            root["items"]?.jsonArray.orEmpty().mapNotNull { element ->
                val obj = element.jsonObject
                val fullName = obj.str("full_name") ?: return@mapNotNull null
                Repo(
                    fullName = fullName,
                    description = obj.str("description").orEmpty(),
                    stars = obj["stargazers_count"]?.jsonPrimitive?.intOrNull ?: 0,
                    defaultBranch = obj.str("default_branch") ?: "main",
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Rate limit, no network, or a changed response shape: an empty result is honest here
            // and the screen says nothing was found.
            emptyList()
        }
    }

    /**
     * Whether [repo] publishes a store this app can add.
     *
     * A repository counts only if one of the usual index paths answers with a body this app's own
     * index parser accepts. Checking the parsed content rather than just the HTTP status keeps a
     * 404 page served with status 200, or a repository that merely mentions extensions, from being
     * offered as a store.
     */
    private suspend fun verify(repo: Repo): StoreCandidate? {
        for (path in INDEX_PATHS) {
            val url = "https://raw.githubusercontent.com/${repo.fullName}/${repo.defaultBranch}/$path"
            val body = try {
                network.client.newCall(GET(url)).awaitSuccess().body.string()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                continue
            }
            if (looksLikeStore(body)) {
                return StoreCandidate(
                    name = repo.fullName,
                    description = repo.description,
                    stars = repo.stars,
                    indexUrl = url,
                )
            }
        }
        return null
    }

    /**
     * The cheapest honest test of "is this an index": it must parse as JSON and carry the one field
     * every store index has. A store's index is a JSON array of entries, a smaller store's is an
     * object wrapping them; both shapes are accepted.
     */
    private fun looksLikeStore(body: String): Boolean {
        val text = body.trimStart()
        if (!text.startsWith("[") && !text.startsWith("{")) return false
        return try {
            val element = json.parseToJsonElement(text)
            when (element) {
                is kotlinx.serialization.json.JsonArray -> {
                    element.isNotEmpty() && element.first().jsonObject.containsKey("pkg")
                }
                else -> {
                    val obj = element.jsonObject
                    // Either it already lists extensions, or it points at the newer index.
                    obj.containsKey("extensions") || obj.containsKey("indexV2") || obj.containsKey("name")
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")

    private data class Repo(
        val fullName: String,
        val description: String,
        val stars: Int,
        val defaultBranch: String,
    )

    private companion object {
        /**
         * Where a store's index is known to live, most common first. Two are enough to cover the
         * layout every Mihon store in the wild uses; a wrong guess costs one request that 404s.
         */
        val INDEX_PATHS = listOf("index.min.json", "repo.json")
    }
}

/** One repository that passed verification, ready to be offered as a store. */
data class StoreCandidate(
    val name: String,
    val description: String,
    val stars: Int,
    val indexUrl: String,
)
