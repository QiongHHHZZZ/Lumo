package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import mihon.icons.custommaterialsymbols.CustomMaterialSymbols
import mihon.icons.custommaterialsymbols.rounded.CropOff
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Crop
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The reader's bottom bar: the things that belong to the comic being read, each an icon over its
 * label.
 *
 * Everything here is per-series — the reading mode, the screen orientation and whether the page is
 * cropped — so the labels say "read" and "screen" rather than repeating the setting names found in
 * the app's settings, which are global and live there instead.
 */
@Composable
fun ReaderBottomBar(
    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReaderBarButton(
            modifier = Modifier.weight(1f),
            icon = readingMode.icon,
            // "Read", not the mode name: the mode already lives in the app's settings, and this
            // button picks it for this comic only.
            label = stringResource(MR.strings.pref_reader_page_read),
            onClick = onClickReadingMode,
        )

        ReaderBarButton(
            modifier = Modifier.weight(1f),
            icon = orientation.icon,
            label = stringResource(MR.strings.pref_reader_page_screen),
            onClick = onClickOrientation,
        )

        ReaderBarButton(
            modifier = Modifier.weight(1f),
            icon = if (cropEnabled) {
                MaterialSymbols.Rounded.Crop
            } else {
                CustomMaterialSymbols.Rounded.CropOff
            },
            // The state is the useful part here: the icon alone reads as "crop" whether it is on
            // or off.
            label = stringResource(
                if (cropEnabled) MR.strings.pref_crop_borders_on else MR.strings.pref_crop_borders_off,
            ),
            onClick = onClickCropBorder,
            selected = cropEnabled,
        )
    }
}

@Composable
private fun ReaderBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Column(
        modifier = modifier
            // Generous vertical padding keeps the tap target comfortable at this text size.
            .padding(vertical = MaterialTheme.padding.small)
            .selectable(selected = selected, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
