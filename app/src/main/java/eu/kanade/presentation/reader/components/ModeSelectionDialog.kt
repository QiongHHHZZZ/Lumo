package eu.kanade.presentation.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ModeSelectionDialog(
    onApply: () -> Unit,
    onReset: (() -> Unit)? = null,
    resetLabel: String? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.padding(vertical = 16.dp)) {
        Column {
            // These dialogs only ever set a per-series value, and the app's settings hold the
            // global default. Saying so here stops the two from being confused.
            Text(
                text = stringResource(MR.strings.pref_reader_only_for_this_series),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = SettingsItemsPaddings.Horizontal,
                    end = SettingsItemsPaddings.Horizontal,
                    bottom = MaterialTheme.padding.small,
                ),
            )

            content()

            Row(
                modifier = Modifier.padding(
                    horizontal = SettingsItemsPaddings.Horizontal,
                ),
            ) {
                // Not a "default" choice in the list: an explicit way back to following the app's
                // setting, which is a different thing from picking a value.
                if (onReset != null && resetLabel != null) {
                    OutlinedButton(onClick = onReset) {
                        Text(text = resetLabel)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                FilledTonalButton(
                    onClick = onApply,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                        )
                        Text(text = stringResource(MR.strings.action_apply))
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    TachiyomiPreviewTheme {
        Surface {
            Column {
                ModeSelectionDialog(onApply = {}) {
                    Text("Dummy content")
                }
            }
        }
    }
}
