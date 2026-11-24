package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.luma.di.appGraph
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * How the reader looks while a page is open: background, status line, hints.
 *
 * One flat group, no heading of its own — the page title already says "Display", and a group with
 * the same heading underneath put the same word twice on the screen.
 */
object ReaderDisplaySettingsScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_reader_page_display

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val readerPref = remember { context.appGraph.readerPreferences }

        return listOf(
            Preference.PreferenceGroup(
                // Needed, and worded differently from the page title ("显示"), so the rows are
                // grouped without the heading reading as a repeat.
                title = stringResource(MR.strings.pref_reader_group_appearance),
                preferenceItems = listOf(
                    Preference.PreferenceItem.ListPreference(
                        preference = readerPref.defaultOrientationType,
                        entries = ReaderOrientation.entries.drop(1)
                            .associate { it.flagValue to stringResource(it.stringRes) },
                        title = stringResource(MR.strings.pref_rotation_type),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = readerPref.readerTheme,
                        entries = mapOf(
                            1 to stringResource(MR.strings.black_background),
                            2 to stringResource(MR.strings.gray_background),
                            0 to stringResource(MR.strings.white_background),
                            3 to stringResource(MR.strings.automatic_background),
                        ),
                        title = stringResource(MR.strings.pref_reader_theme),
                        subtitle = stringResource(MR.strings.pref_reader_theme_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPref.keepScreenOn,
                        title = stringResource(MR.strings.pref_keep_screen_on),
                        subtitle = stringResource(MR.strings.pref_keep_screen_on_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPref.showPageNumber,
                        title = stringResource(MR.strings.pref_show_page_number),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPref.showReadingClock,
                        title = stringResource(MR.strings.pref_show_reading_clock),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPref.showReadingBattery,
                        title = stringResource(MR.strings.pref_show_reading_battery),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPref.showReadingMode,
                        title = stringResource(MR.strings.pref_show_reading_mode),
                        subtitle = stringResource(MR.strings.pref_show_reading_mode_summary),
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = readerPref.showNavigationOverlayOnStart,
                        title = stringResource(MR.strings.pref_show_navigation_mode),
                        subtitle = stringResource(MR.strings.pref_show_navigation_mode_summary),
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = readerPref.doubleTapAnimSpeed,
                        entries = mapOf(
                            1 to stringResource(MR.strings.double_tap_anim_speed_0),
                            500 to stringResource(MR.strings.double_tap_anim_speed_normal),
                            250 to stringResource(MR.strings.double_tap_anim_speed_fast),
                        ),
                        title = stringResource(MR.strings.pref_double_tap_anim_speed),
                    ),
                ),
            ),
        )
    }
}
