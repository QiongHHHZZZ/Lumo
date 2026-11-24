package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.luma.di.appGraph
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat

/**
 * Colour work and e-ink. The high-quality renderer settings stay on the reader page itself, since
 * that switch has to remain reachable while its group is hidden.
 */
object ReaderAdvancedSettingsScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_reader_page_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val readerPref = remember { context.appGraph.readerPreferences }

        return listOf(
            // A heading is needed: it is what says those rows are about colour. It is worded
            // differently from the page title so the two do not read as a repeat of each other.
            getColorFilterGroup(readerPreferences = readerPref),
            getEInkGroup(readerPreferences = readerPref),
        )
    }

    @Composable
    private fun getColorFilterGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_group_colour),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_reader_group_colour),
                ) {
                    ColorFilterSettings(readerPreferences)
                },
            ),
        )
    }

    @Composable
    private fun getEInkGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
        val flashPageState by readerPreferences.flashOnPageChange.collectAsState()

        val flashColorPref = readerPreferences.flashColor

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_group_eink),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readerPreferences.flashOnPageChange,
                    title = stringResource(MR.strings.pref_flash_page),
                    subtitle = stringResource(MR.strings.pref_flash_page_summ),
                ),
                Preference.PreferenceItem.SliderPreference(
                    preference = readerPreferences.flashDurationMillis,
                    valueRange = ReaderPreferences.MILLI_CONVERSION.let { it..it * 15 step it },
                    title = stringResource(MR.strings.pref_flash_duration),
                    valueText = { stringResource(MR.strings.pref_flash_duration_summary, it) },
                    visible = flashPageState,
                ),
                Preference.PreferenceItem.SliderPreference(
                    preference = readerPreferences.flashPageInterval,
                    valueRange = 1..10,
                    title = stringResource(MR.strings.pref_flash_page_interval),
                    valueText = { pluralStringResource(MR.plurals.pref_pages, it, it) },
                    visible = flashPageState,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = flashColorPref,
                    entries = mapOf(
                        ReaderPreferences.FlashColor.BLACK to stringResource(MR.strings.pref_flash_style_black),
                        ReaderPreferences.FlashColor.WHITE to stringResource(MR.strings.pref_flash_style_white),
                        ReaderPreferences.FlashColor.WHITE_BLACK
                            to stringResource(MR.strings.pref_flash_style_white_black),
                    ),
                    title = stringResource(MR.strings.pref_flash_with),
                    visible = flashPageState,
                ),
            ),
        )
    }
}
