package eu.kanade.domain.source.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet

@Inject
class ToggleLanguage(
    val preferences: SourcePreferences,
) {

    fun await(language: String) {
        val isEnabled = language in preferences.enabledLanguages.get()
        preferences.enabledLanguages.getAndSet { enabled ->
            if (isEnabled) enabled.minus(language) else enabled.plus(language)
        }
    }

    /** Enables or disables every one of [languages] in a single write. */
    fun await(languages: Set<String>, enable: Boolean) {
        preferences.enabledLanguages.getAndSet { enabled ->
            if (enable) enabled.plus(languages) else enabled.minus(languages)
        }
    }
}
