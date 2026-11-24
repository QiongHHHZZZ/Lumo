package eu.kanade.domain.source.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.isLocal
import java.util.SortedMap

@Inject
class GetLanguagesWithSources(
    private val repository: SourceRepository,
    private val preferences: SourcePreferences,
) {

    fun subscribe(): Flow<SortedMap<String, List<Source>>> {
        return combine(
            preferences.enabledLanguages.changes(),
            preferences.disabledSources.changes(),
            repository.getSources(),
        ) { enabledLanguage, disabledSource, sources ->
            // Local source is included so it can be re-enabled from this screen after being
            // disabled via its options menu. It is not an online source, so filtering to
            // online-only sources would otherwise leave it unreachable once disabled.
            val sortedSources = sources
                .sortedWith(
                    compareBy<Source> { it.id.toString() in disabledSource }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                )

            sortedSources
                // "other" is the language the local source reports; keep it grouped properly
                // instead of dropping it.
                .groupBy { it.lang }
                .toSortedMap(
                    compareBy<String> { it !in enabledLanguage }.then(LocaleHelper.comparator),
                )
        }
    }
}
