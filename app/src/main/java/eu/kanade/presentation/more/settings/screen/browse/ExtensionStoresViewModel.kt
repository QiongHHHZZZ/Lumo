package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.domain.extension.interactor.AddExtensionStore
import mihon.domain.extension.interactor.GetExtensionStores
import mihon.domain.extension.interactor.RemoveExtensionStore
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.model.ExtensionStore
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ExtensionStoresViewModel(
    private val getExtensionStores: GetExtensionStores,
    private val addExtensionStore: AddExtensionStore,
    private val removeExtensionStore: RemoveExtensionStore,
    private val updateExtensionStores: UpdateExtensionStores,
    private val extensionManager: ExtensionManager,
    private val gitHubStoreSearch: GitHubStoreSearch,
) : ViewModel() {

    private val dialog = MutableStateFlow<ExtensionStoreDialog?>(null)

    /** The GitHub search dialog's own state; null while it is closed. */
    private val search = MutableStateFlow<StoreSearchState?>(null)

    val searchState: StateFlow<StoreSearchState?> = search

    val state: StateFlow<ExtensionStoreScreenState> = combine(
        getExtensionStores.subscribe(),
        dialog,
    ) { stores, dialog ->
        ExtensionStoreScreenState.Success(stores = stores, dialog = dialog)
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), ExtensionStoreScreenState.Loading)

    /**
     * Creates and adds a new repo to the database.
     *
     * @param baseUrl The baseUrl of the repo to create.
     */
    fun createRepo(baseUrl: String) {
        viewModelScope.launch {
            dialog.update {
                when (it) {
                    is ExtensionStoreDialog.Create -> it.copy(processing = true)
                    is ExtensionStoreDialog.Confirm -> it.copy(processing = true)
                    else -> it
                }
            }
            addExtensionStore(baseUrl)
                .onSuccess {
                    extensionManager.findAvailableExtensions()
                    dismissDialog()
                }
                .onFailure { throwable ->
                    val message = throwable.toUserMessage()
                    dialog.update {
                        when (it) {
                            is ExtensionStoreDialog.Create -> it.copy(
                                processing = false,
                                errorMessage = message,
                            )
                            is ExtensionStoreDialog.Confirm -> it.copy(
                                processing = false,
                                errorMessage = message,
                            )
                            else -> it
                        }
                    }
                }
        }
    }

    /**
     * Refreshes information for each repository.
     */
    fun refreshRepos() {
        viewModelScope.launch(Dispatchers.IO) {
            updateExtensionStores()
        }
    }

    /**
     * Deletes the given repo from the database
     */
    fun deleteRepo(baseUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            removeExtensionStore(baseUrl)
            extensionManager.findAvailableExtensions()
        }
    }

    fun addFromDeeplink(storeIndexUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val alreadyExists = getExtensionStores.get().any { it.indexUrl == storeIndexUrl }
            dialog.update { ExtensionStoreDialog.Confirm(url = storeIndexUrl, alreadyExists = alreadyExists) }
        }
    }

    fun showDialog(dialog: ExtensionStoreDialog) {
        this.dialog.update { dialog }
    }

    fun dismissDialog() {
        dialog.update { null }
    }

    // region GitHub search

    fun openSearch() {
        search.value = StoreSearchState()
    }

    fun closeSearch() {
        search.value = null
    }

    fun updateSearchQuery(query: String) {
        search.update { it?.copy(query = query) ?: StoreSearchState(query = query) }
    }

    /**
     * Runs one search. GitHub's anonymous search allows ten requests a minute, so this is a button
     * the user presses rather than something that runs as they type.
     */
    fun runSearch() {
        val current = search.value ?: return
        if (current.running) return
        search.update { it?.copy(running = true, results = emptyList(), checked = 0, total = 0) }

        viewModelScope.launch {
            val results = gitHubStoreSearch.search(current.query) { checked, total ->
                search.update { it?.copy(checked = checked, total = total) }
            }
            search.update { it?.copy(running = false, results = results) }
        }
    }

    // endregion
}

sealed class ExtensionStoreDialog {
    data class Create(val processing: Boolean = false, val errorMessage: String? = null) : ExtensionStoreDialog()
    data class Delete(val store: ExtensionStore) : ExtensionStoreDialog()
    data class Confirm(
        val url: String,
        val alreadyExists: Boolean = false,
        val processing: Boolean = false,
        val errorMessage: String? = null,
    ) : ExtensionStoreDialog()
}

/**
 * Turns the exception thrown while adding a repository into something a user can act on. Network
 * exceptions stringify to bare technical words like "timeout", which say neither what went wrong
 * nor what to do about it. The underlying detail is appended because the failure can also come from
 * a proxy serving something unexpected, and that case is indistinguishable without it.
 */
private fun Throwable.toUserMessage(): String {
    val hint = when (this) {
        is java.net.SocketTimeoutException,
        is java.net.ConnectException,
        -> "连接超时，请检查网络后重试"
        is java.net.UnknownHostException -> "找不到这个地址，请检查是否输入正确"
        is javax.net.ssl.SSLException -> "安全连接失败，请检查网络环境"
        is java.io.IOException -> "服务器没有返回可用的插件清单"
        else -> "获取插件仓库失败，请检查地址是否正确"
    }
    val detail = message?.takeIf { it.isNotBlank() } ?: this::class.simpleName.orEmpty()
    return if (detail.isBlank()) hint else "$hint\n（$detail）"
}

sealed class ExtensionStoreScreenState {

    @Immutable
    data object Loading : ExtensionStoreScreenState()

    @Immutable
    data class Success(
        val stores: List<ExtensionStore>,
        val dialog: ExtensionStoreDialog? = null,
    ) : ExtensionStoreScreenState() {

        val isEmpty: Boolean
            get() = stores.isEmpty()
    }
}

/**
 * What the GitHub search dialog is doing.
 *
 * [checked] and [total] are counts of *repositories probed*, not search results: verification is
 * the slow half, and a bar that moves only when the search request returns would sit still for the
 * whole of it.
 */
@Immutable
data class StoreSearchState(
    val query: String = DEFAULT_QUERY,
    val running: Boolean = false,
    val checked: Int = 0,
    val total: Int = 0,
    val results: List<StoreCandidate> = emptyList(),
) {
    companion object {
        /**
         * The words that actually appear in the names of extension stores. @see GitHubStoreSearch
         * for why there is no list of known stores instead.
         */
        const val DEFAULT_QUERY = "tachiyomi extension in:name,description,readme"
    }
}
