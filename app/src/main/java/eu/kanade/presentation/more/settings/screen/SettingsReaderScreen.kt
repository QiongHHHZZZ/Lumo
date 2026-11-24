package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The way into the reader's settings.
 *
 * This page used to be one long scroll: ten groups, sixty-odd switches and two entries that looked
 * like navigation but were settings. It is now five destinations, each of which fits on about a
 * screen, and every row on this page does the same thing: open one of them.
 */
object SettingsReaderScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_reader

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_reader),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_reader_page_display),
                        subtitle = stringResource(MR.strings.pref_reader_page_display_summary),
                        onClick = { navigator.push(ReaderDisplaySettingsScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_reader_page_modes),
                        subtitle = stringResource(MR.strings.pref_reader_page_modes_summary),
                        onClick = { navigator.push(ReaderModesSettingsScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_reader_page_controls),
                        subtitle = stringResource(MR.strings.pref_reader_page_controls_summary),
                        onClick = { navigator.push(ReaderControlsSettingsScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_reader_page_advanced),
                        subtitle = stringResource(MR.strings.pref_reader_page_advanced_summary),
                        onClick = { navigator.push(ReaderAdvancedSettingsScreen) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_category_high_quality),
                        subtitle = stringResource(MR.strings.pref_category_high_quality_summary),
                        onClick = { navigator.push(ReaderHighQualitySettingsScreen) },
                    ),
                ),
            ),
        )
    }
}
