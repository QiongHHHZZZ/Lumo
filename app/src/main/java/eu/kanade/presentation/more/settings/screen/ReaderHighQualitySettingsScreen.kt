package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.luma.di.appGraph
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.viewer.webgpu.WebGpuRenderingSettings
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * The high-quality renderer: its switch, and the shader work it enables.
 *
 * Upstream marks this renderer experimental and ships it off by default. Keeping it on a page of
 * its own means the switches that only work when it is on are not scattered through the reader's
 * ordinary settings, and that the page still opens — with the switch and an explanation — when it
 * is off.
 */
object ReaderHighQualitySettingsScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_high_quality

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val readerPref = remember { context.appGraph.readerPreferences }
        val basePreferences = remember { context.appGraph.basePreferences }
        val highQualityRenderer by basePreferences.highQualityRenderer.collectAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_renderer),
                preferenceItems = buildList {
                    add(
                        Preference.PreferenceItem.SwitchPreference(
                            preference = basePreferences.highQualityRenderer,
                            title = stringResource(MR.strings.pref_high_quality_renderer),
                            subtitle = stringResource(MR.strings.pref_high_quality_renderer_summary),
                        ),
                    )
                    // Only while it is off: once on, the settings below say what it does.
                    if (!highQualityRenderer) {
                        add(
                            Preference.PreferenceItem.InfoPreference(
                                stringResource(MR.strings.pref_high_quality_renderer_experimental),
                            ),
                        )
                    }
                },
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_rendering),
                visible = highQualityRenderer,
                preferenceItems = listOf(
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(MR.strings.pref_category_rendering),
                    ) {
                        WebGpuRenderingSettings(readerPref)
                    },
                ),
            ),
        )
    }
}
