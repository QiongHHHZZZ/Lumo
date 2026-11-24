package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.extension.interactor.GetExtensionLanguages
import eu.kanade.domain.source.interactor.ToggleLanguage
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.time.Duration.Companion.seconds

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class ExtensionFilterViewModel(
    private val preferences: SourcePreferences,
    private val getExtensionLanguages: GetExtensionLanguages,
    private val toggleLanguage: ToggleLanguage,
    private val context: android.content.Context,
) : ViewModel() {

    private val _events: Channel<ExtensionFilterEvent> = Channel()
    val events: Flow<ExtensionFilterEvent> = _events.receiveAsFlow()

    /** Filters the language list by its displayed name. */
    val searchQuery = MutableStateFlow<String?>(null)

    val state: StateFlow<ExtensionFilterState> = combine(
        getExtensionLanguages.subscribe(),
        preferences.enabledLanguages.changes(),
        searchQuery,
    ) { extensionLanguages, enabledLanguages, query ->
        val visible = if (query.isNullOrBlank()) {
            extensionLanguages
        } else {
            extensionLanguages.filter {
                LocaleHelper.getSourceDisplayName(it, context).contains(query, ignoreCase = true)
            }
        }
        ExtensionFilterState.Success(
            languages = visible,
            allLanguages = extensionLanguages,
            enabledLanguages = enabledLanguages,
        )
    }
        .catch { throwable ->
            logcat(LogPriority.ERROR, throwable)
            _events.send(ExtensionFilterEvent.FailedFetchingLanguages)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), ExtensionFilterState.Loading)

    fun search(query: String?) {
        searchQuery.value = query
    }

    fun toggle(language: String) {
        toggleLanguage.await(language)
    }

    /** Applies to every language, not just the ones a search currently shows. */
    fun toggleAll(enable: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val languages = (state.value as? ExtensionFilterState.Success)?.allLanguages.orEmpty()
            toggleLanguage.await(languages.toSet(), enable)
        }
    }
}

sealed interface ExtensionFilterEvent {
    data object FailedFetchingLanguages : ExtensionFilterEvent
}

sealed interface ExtensionFilterState {

    @Immutable
    data object Loading : ExtensionFilterState

    @Immutable
    data class Success(
        val languages: List<String>,
        /** Every language available, regardless of the current search. */
        val allLanguages: List<String> = languages,
        val enabledLanguages: Set<String> = setOf(),
    ) : ExtensionFilterState {

        val isEmpty: Boolean
            get() = languages.isEmpty()
    }
}
