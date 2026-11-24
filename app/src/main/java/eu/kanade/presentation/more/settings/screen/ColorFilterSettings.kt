package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.Companion.ColorFilterMode
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * Screen brightness, a colour wash, grayscale and inversion.
 *
 * These used to be reachable only from inside the reader. They are global — they apply to every
 * comic — so they live in Settings now, and the reader's own panel is gone.
 *
 * Wrapped in a [Column]: the rows below are built for a vertical list and give themselves no height
 * of their own, so without this they collapse on top of each other.
 */
@Composable
fun ColorFilterSettings(preferences: ReaderPreferences) {
    Column {
        val customBrightness by preferences.customBrightness.collectAsState()
        CheckboxItem(
            label = stringResource(MR.strings.pref_custom_brightness),
            pref = preferences.customBrightness,
        )

        /*
         * Sets the brightness of the screen. Range is [-75, 100].
         * From -75 to -1 a semi-transparent black view is shown at the top with the minimum brightness.
         * From 1 to 100 it sets that value as brightness.
         * 0 sets system brightness and hides the overlay.
         */
        if (customBrightness) {
            val customBrightnessValue by preferences.customBrightnessValue.collectAsState()
            SliderItem(
                value = customBrightnessValue,
                valueRange = -75..100,
                steps = 0,
                label = stringResource(MR.strings.pref_custom_brightness),
                onChange = { preferences.customBrightnessValue.set(it) },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }

        val colorFilter by preferences.colorFilter.collectAsState()
        CheckboxItem(
            label = stringResource(MR.strings.pref_custom_color_filter),
            pref = preferences.colorFilter,
        )
        if (colorFilter) {
            val colorFilterValue by preferences.colorFilterValue.collectAsState()
            SliderItem(
                value = colorFilterValue.red,
                valueRange = 0..255,
                steps = 0,
                label = stringResource(MR.strings.color_filter_r_value),
                onChange = { newRValue ->
                    preferences.colorFilterValue.getAndSet {
                        getColorValue(it, newRValue, RED_MASK, 16)
                    }
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            SliderItem(
                value = colorFilterValue.green,
                valueRange = 0..255,
                steps = 0,
                label = stringResource(MR.strings.color_filter_g_value),
                onChange = { newGValue ->
                    preferences.colorFilterValue.getAndSet {
                        getColorValue(it, newGValue, GREEN_MASK, 8)
                    }
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            SliderItem(
                value = colorFilterValue.blue,
                valueRange = 0..255,
                steps = 0,
                label = stringResource(MR.strings.color_filter_b_value),
                onChange = { newBValue ->
                    preferences.colorFilterValue.getAndSet {
                        getColorValue(it, newBValue, BLUE_MASK, 0)
                    }
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            SliderItem(
                value = colorFilterValue.alpha,
                valueRange = 0..255,
                steps = 0,
                label = stringResource(MR.strings.color_filter_a_value),
                onChange = { newAValue ->
                    preferences.colorFilterValue.getAndSet {
                        getColorValue(it, newAValue, ALPHA_MASK, 24)
                    }
                },
                pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            val colorFilterMode by preferences.colorFilterMode.collectAsState()
            SettingsChipRow(MR.strings.pref_color_filter_mode) {
                ColorFilterMode.mapIndexed { index, it ->
                    FilterChip(
                        selected = colorFilterMode == index,
                        onClick = { preferences.colorFilterMode.set(index) },
                        label = { Text(stringResource(it.first)) },
                    )
                }
            }
        }

        CheckboxItem(
            label = stringResource(MR.strings.pref_grayscale),
            pref = preferences.grayscale,
        )
        CheckboxItem(
            label = stringResource(MR.strings.pref_inverted_colors),
            pref = preferences.invertedColors,
        )
    }
}

private fun getColorValue(currentColor: Int, color: Int, mask: Long, bitShift: Int): Int {
    return (color shl bitShift) or (currentColor and mask.inv().toInt())
}
private const val ALPHA_MASK: Long = 0xFF000000
private const val RED_MASK: Long = 0x00FF0000
private const val GREEN_MASK: Long = 0x0000FF00
private const val BLUE_MASK: Long = 0x000000FF
