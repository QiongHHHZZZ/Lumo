package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat

/**
 * How the high-quality renderer turns pages and lays out a long strip.
 *
 * These are not shader settings — they decide what a page change looks like and where the next page
 * starts, so they belong next to the other reading-mode choices, not behind the rendering group.
 * Everything here only matters while the renderer is on; the page that hosts this says so.
 */
@Composable
fun WebGpuViewBehaviourSettings(preferences: ReaderPreferences) {
    Column {
        val dualPageView by preferences.dualPageView.collectAsState()
        SettingsChipRow(MR.strings.pref_dual_page_view) {
            ReaderPreferences.DualPageView.entries.forEach {
                FilterChip(
                    selected = it == dualPageView,
                    onClick = { preferences.dualPageView.set(it) },
                    label = { Text(stringResource(it.titleRes)) },
                )
            }
        }

        val cutoutMode by preferences.cutoutMode.collectAsState()
        SettingsChipRow(MR.strings.pref_cutout_mode) {
            ReaderPreferences.CutoutMode.entries.forEach {
                FilterChip(
                    selected = it == cutoutMode,
                    onClick = { preferences.cutoutMode.set(it) },
                    label = { Text(stringResource(it.titleRes)) },
                )
            }
        }

        val cutoutModeDual by preferences.cutoutModeDual.collectAsState()
        SettingsChipRow(MR.strings.pref_cutout_mode_dual) {
            ReaderPreferences.CutoutMode.entries.forEach {
                FilterChip(
                    selected = it == cutoutModeDual,
                    onClick = { preferences.cutoutModeDual.set(it) },
                    label = { Text(stringResource(it.titleRes)) },
                )
            }
        }

        val transitionAnimation by preferences.transitionAnimation.collectAsState()
        SettingsChipRow(MR.strings.pref_transition_animation) {
            ReaderPreferences.TransitionAnimation.entries.forEach {
                FilterChip(
                    selected = it == transitionAnimation,
                    onClick = { preferences.transitionAnimation.set(it) },
                    label = { Text(stringResource(it.titleRes)) },
                )
            }
        }

        val transitionAnimationDual by preferences.transitionAnimationDual.collectAsState()
        SettingsChipRow(MR.strings.pref_transition_animation_dual) {
            // The left/right flips only make sense on a single page.
            (
                ReaderPreferences.TransitionAnimation.entries - ReaderPreferences.TransitionAnimation.FLIP_LEFT -
                    ReaderPreferences.TransitionAnimation.FLIP_RIGHT
                ).forEach {
                FilterChip(
                    selected = it == transitionAnimationDual,
                    onClick = { preferences.transitionAnimationDual.set(it) },
                    label = { Text(stringResource(it.titleRes)) },
                )
            }
        }

        val dualPageRotateToFit by preferences.dualPageRotateToFit.collectAsState()
        CheckboxItem(
            label = stringResource(MR.strings.pref_page_rotate),
            pref = preferences.dualPageRotateToFit,
        )
        if (dualPageRotateToFit) {
            CheckboxItem(
                label = stringResource(MR.strings.pref_page_rotate_invert),
                pref = preferences.dualPageRotateToFitInvert,
            )
        }

        CheckboxItem(
            label = stringResource(MR.strings.pref_crop_instead_of_zoom),
            pref = preferences.cropInsteadOfZoom,
        )

        HeadingItem(MR.strings.pref_continuous_layout)
        val numberFormat = remember { NumberFormat.getPercentInstance() }
        val continuousMinWidth by preferences.continuousMinWidth.collectAsState()
        SliderItem(
            value = continuousMinWidth,
            valueRange = 1..100,
            label = stringResource(MR.strings.pref_continuous_minwidth),
            valueString = numberFormat.format(continuousMinWidth / 100f),
            onChange = { preferences.continuousMinWidth.set(it) },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        val continuousGap by preferences.continuousGap.collectAsState()
        SliderItem(
            value = continuousGap,
            valueRange = 1..100,
            label = stringResource(MR.strings.pref_continuous_gap),
            valueString = numberFormat.format(continuousGap / 100f),
            onChange = { preferences.continuousGap.set(it) },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}
