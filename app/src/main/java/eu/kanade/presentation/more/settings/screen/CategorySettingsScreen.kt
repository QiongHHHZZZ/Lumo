package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.luma.di.appGraph
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Category behaviour, reached from the category screen itself rather than from Library settings.
 * Editing the category list lives one step away, so having these next to it keeps category work in
 * one place instead of split across two menus.
 */
object CategorySettingsScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.category_settings

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val libraryPreferences = remember { context.appGraph.libraryPreferences }
        val getCategories = remember { context.appGraph.getCategories }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        val ids = listOf(libraryPreferences.defaultCategory.defaultValue()) +
            allCategories.map { it.id.toInt() }
        val labels = listOf(stringResource(MR.strings.default_category_summary)) +
            allCategories.map { it.visualName }

        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = libraryPreferences.defaultCategory,
                entries = ids.zip(labels).toMap(),
                title = stringResource(MR.strings.default_category),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.categorizedDisplaySettings,
                title = stringResource(MR.strings.categorized_display_settings),
                onValueChanged = {
                    if (!it) {
                        scope.launch {
                            context.appGraph.resetCategoryFlags.await()
                        }
                    }
                    true
                },
            ),
        )
    }
}
