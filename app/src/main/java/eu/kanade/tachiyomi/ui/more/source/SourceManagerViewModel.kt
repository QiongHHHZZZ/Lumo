package eu.kanade.tachiyomi.ui.more.source

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Backs the extension management screen, modelled on VeneraQ's source page: every extension is a
 * card carrying its own facts (version, update state, source count) and actions, so nothing needs a
 * detail screen to be understood.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class SourceManagerViewModel(
    private val extensionManager: ExtensionManager,
    private val getExtensions: GetExtensionsByType,
    private val getExtensionSources: GetExtensionSources,
    private val toggleSource: ToggleSource,
    private val basePreferences: BasePreferences,
) : ViewModel() {

    private val currentDownloads = MutableStateFlow<Map<String, InstallStep>>(emptyMap())

    private val isRefreshing = MutableStateFlow(false)

    /** Which tab is showing: installed extensions, or what can still be installed. */
    val showInstalled = MutableStateFlow(true)

    /** Search text, matched against extension names, source names, home URLs and source ids. */
    val searchQuery = MutableStateFlow<String?>(null)

    val state: StateFlow<State> = combine(
        getExtensions.subscribe(),
        combine(currentDownloads, isRefreshing, basePreferences.extensionInstaller.changes()) { d, r, i ->
            Triple(d, r, i)
        },
        searchQuery,
    ) { extensions, (downloads, refreshing, installer), query ->
        val predicate = searchPredicate(query)
        State(
            isRefreshing = refreshing,
            installer = installer,
            // 待更新的插件在数据层被单独挑出去了（loaded / notLoaded 里不含它们），
            // 所以这里要显式拼回来，否则它们从列表里消失，用户就点不到单独更新。
            installed = (extensions.updates + extensions.loaded + extensions.notLoaded)
                .filter(predicate)
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                .map { it.toCard(downloads) },
            available = extensions.available
                .filter(predicate)
                .map { it.toCard(downloads) },
            updates = extensions.updates.size,
            searchQuery = query,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5.seconds),
            State(isLoading = true),
        )

    /**
     * Matches an extension by name, or any of its sources by name, home URL or id. Comma-separated
     * terms are OR'd, matching how Browse's extension search behaved.
     */
    private fun searchPredicate(query: String?): (Extension) -> Boolean {
        val subqueries = query
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        if (subqueries.isEmpty()) return { true }

        return { extension ->
            subqueries.any { subquery ->
                extension.name.contains(subquery, ignoreCase = true) ||
                    when (extension) {
                        is Extension.Loaded -> extension.sources.any { source ->
                            source.name.contains(subquery, ignoreCase = true) ||
                                (source as? HttpSource)?.getHomeUrl()?.contains(subquery, ignoreCase = true) == true ||
                                source.id == subquery.toLongOrNull()
                        }
                        is Extension.Available -> extension.sources.any {
                            it.name.contains(subquery, ignoreCase = true) ||
                                it.baseUrl.contains(subquery, ignoreCase = true) ||
                                it.id == subquery.toLongOrNull()
                        }
                        else -> false
                    }
            }
        }
    }

    fun search(query: String?) {
        searchQuery.value = query
    }

    /** Re-fetches every configured store's listing. */
    fun refresh() {
        refreshAvailable()
    }

    /**
     * 跑一个插件的更新/安装流程，并把每一步反映到界面上。
     *
     * 装好后停 1.5 秒再清掉这行状态：直接清会让进度条瞬间消失，
     * 用户看不出到底成没成。失败状态则留着，原因可以被读到、被截图。
     * 单装和批量更新都走这里，两条路的表现才一致。
     */
    private suspend fun runInstallFlow(extension: Extension) {
        extensionManager.updateExtension(extension as Extension.Installed).collect { step ->
            currentDownloads.update { it + (extension.pkgName to step) }
            when {
                step == InstallStep.Installed -> {
                    delay(1500)
                    currentDownloads.update { it - extension.pkgName }
                }
                step is InstallStep.Error -> Unit
            }
        }
    }

    /**
     * 更新所有有新版可装的插件。
     *
     * 这里刻意不读 [state]：state 里的 installed 列表已经过搜索词过滤，
     * 搜索框里只要还留着字，列表就可能不含待更新的项，按钮就成了摆设。
     * 直接问 [ExtensionManager] 要待更新的清单，与搜索状态无关。
     */
    fun updateAll() {
        viewModelScope.launch(Dispatchers.IO) {
            getExtensions.subscribe()
                .first()
                .updates
                .filterIsInstance<Extension.Installed>()
                .forEach { extension ->
                    runInstallFlow(extension)
                }
        }
    }

    private val sourceFlows = mutableMapOf<String, StateFlow<List<ExtensionSourceItem>>>()

    /** Enabled/disabled state of each source inside [extension], resolved once per package. */
    fun sourcesFor(extension: Extension.Loaded): StateFlow<List<ExtensionSourceItem>> {
        return sourceFlows.getOrPut(extension.pkgName) {
            getExtensionSources.subscribe(extension)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), emptyList())
        }
    }

    fun setInstalledTab(installed: Boolean) {
        showInstalled.value = installed
    }

    fun toggleSourceEnabled(item: ExtensionSourceItem) {
        toggleSource.await(item.source.id, !item.enabled)
    }

    fun install(extension: Extension.Available) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionManager.installExtension(extension).collect { step ->
                currentDownloads.update { it + (extension.pkgName to step) }
                when {
                    // 成功：短暂显示一下就清掉，因为列表里会多出这个插件，本身已经说明了结果。
                    step == InstallStep.Installed -> {
                        delay(1500)
                        currentDownloads.update { it - extension.pkgName }
                    }
                    // 失败：留着。原因要能被读到、能被截图，所以不设超时——
                    // 用户点一下卡片或者重新装的时候才清掉。
                    step is InstallStep.Error -> Unit
                }
            }
        }
    }

    /** 关掉某一行显示的失败提示。 */
    fun dismissError(pkgName: String) {
        currentDownloads.update { it - pkgName }
    }

    fun update(extension: Extension.Installed) {
        viewModelScope.launch(Dispatchers.IO) {
            runInstallFlow(extension)
        }
    }

    fun uninstall(extension: Extension.Installed) {
        extensionManager.uninstallExtension(extension)
    }

    fun trust(extension: Extension.NotLoaded) {
        viewModelScope.launch(Dispatchers.IO) {
            extensionManager.trust(extension)
        }
    }

    fun cancel(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    fun refreshAvailable() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                extensionManager.findAvailableExtensions()
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private fun Extension.toCard(
        downloads: Map<String, InstallStep>,
    ): ExtensionCard = ExtensionCard(
        extension = this,
        installStep = downloads[pkgName] ?: InstallStep.Idle,
    )

    @Immutable
    data class ExtensionCard(
        val extension: Extension,
        val installStep: InstallStep,
    ) {
        val isInstalled: Boolean get() = extension is Extension.Installed
    }

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val installer: BasePreferences.ExtensionInstaller? = null,
        val installed: List<ExtensionCard> = emptyList(),
        val available: List<ExtensionCard> = emptyList(),
        val updates: Int = 0,
        val searchQuery: String? = null,
    ) {
        val isEmpty: Boolean get() = installed.isEmpty() && available.isEmpty()
    }
}
