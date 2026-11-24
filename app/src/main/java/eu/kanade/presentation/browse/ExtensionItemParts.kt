package eu.kanade.presentation.browse

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.browse.components.ExtensionPill
import eu.kanade.presentation.browse.components.label
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.domain.extension.model.ExtensionStore
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.VerifiedUser
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * 插件卡片的三个组成部分：图标、文字信息、操作按钮。
 *
 * 放在这里而不是某个页面文件里，是因为插件管理页（更多 → 插件）和旧的列表页都要用。
 */

@Composable
internal fun ExtensionItemContent(
    extension: Extension,
    installStep: InstallStep,
    store: ExtensionStore?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium),
    ) {
        Text(
            text = extension.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (store != null) {
            Text(
                text = store.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ExtensionItemMetadata(extension = extension, installStep = installStep)
    }
}

@Composable
internal fun ExtensionItemIcon(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        val idle = installStep.isCompleted()
        if (!idle) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 2.dp,
            )
        }

        val padding by animateDpAsState(
            targetValue = if (idle) 0.dp else 8.dp,
            label = "iconPadding",
        )
        ExtensionIcon(
            extension = extension,
            modifier = Modifier
                .matchParentSize()
                .padding(padding),
        )
    }
}

/**
 * Plain text for what an extension simply is, pills for the few things worth noticing about it.
 * Won't look good when it wraps, but it's not like overflowing content can be ellipsized.
 */
@Composable
private fun ExtensionItemMetadata(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall / 2),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val facts = listOfNotNull(
            (extension as? Extension.Loaded)
                ?.lang
                ?.takeIf { it.isNotEmpty() }
                ?.let { LocaleHelper.getSourceDisplayName(it, LocalContext.current) },
            extension.versionName.takeIf { it.isNotEmpty() },
        )
        if (facts.isNotEmpty()) {
            Text(
                text = facts.joinToString(" \u2022 "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val notable = ArrayList<Pair<dev.icerock.moko.resources.StringResource, androidx.compose.ui.graphics.Color>>()
        when (installStep) {
            InstallStep.Pending -> notable += MR.strings.ext_pending to MaterialTheme.colorScheme.primary
            InstallStep.Downloading -> notable += MR.strings.ext_downloading to MaterialTheme.colorScheme.primary
            InstallStep.Installing -> notable += MR.strings.ext_installing to MaterialTheme.colorScheme.primary
            else -> Unit
        }
        when {
            extension is Extension.NotLoaded ->
                extension.reason.labelRes?.let { notable += it to MaterialTheme.colorScheme.error }
            extension is Extension.Loaded && extension.isObsolete ->
                notable += MR.strings.ext_obsolete to MaterialTheme.colorScheme.error
            else -> Unit
        }
        extension.contentWarning.label?.let { notable += it.title to it.color }
        (extension as? Extension.Loaded)
            ?.takeIf { !it.isShared }
            ?.let { notable += MR.strings.ext_installer_private to MaterialTheme.colorScheme.onSurfaceVariant }
        notable
        notable.forEach { (label, color) ->
            ExtensionPill(text = stringResource(label), color = color)
        }
    }
}

/** 插件没能加载时，在卡片上标出的简短原因；说不清的就不标，详情里再说。 */
private val Extension.NotLoaded.Reason.labelRes: dev.icerock.moko.resources.StringResource?
    get() = when (this) {
        is Extension.NotLoaded.Reason.Untrusted -> MR.strings.ext_untrusted
        Extension.NotLoaded.Reason.Filtered -> MR.strings.ext_filtered
        Extension.NotLoaded.Reason.Unsigned,
        Extension.NotLoaded.Reason.UnsupportedLibVersion,
        Extension.NotLoaded.Reason.Malformed,
        is Extension.NotLoaded.Reason.Failed,
        -> null
    }

@Composable
internal fun ExtensionItemActions(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (Extension) -> Unit = {},
    onClickItemAction: (Extension) -> Unit = {},
    onClickItemSecondaryAction: (Extension) -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!installStep.isCompleted()) {
            IconButton(onClick = { onClickItemCancel(extension) }) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_cancel),
                )
            }
            return@Row
        }

        val secondaryAction = when (extension) {
            is Extension.Available -> {
                (MaterialSymbols.Rounded.Public to MR.strings.action_open_in_web_view)
                    .takeIf { extension.sources.isNotEmpty() }
            }
            is Extension.Loaded -> MaterialSymbols.Rounded.Settings to MR.strings.action_settings
            is Extension.NotLoaded -> if (extension.reason is Extension.NotLoaded.Reason.Untrusted) {
                MaterialSymbols.Rounded.VerifiedUser to MR.strings.ext_trust
            } else {
                MaterialSymbols.Rounded.Info to MR.strings.ext_not_loaded_details
            }
        }
        secondaryAction?.let { (icon, label) ->
            IconButton(onClick = { onClickItemSecondaryAction(extension) }) {
                Icon(imageVector = icon, contentDescription = stringResource(label))
            }
        }

        val action = when {
            installStep is InstallStep.Error -> MaterialSymbols.Rounded.Refresh to MR.strings.action_retry
            extension is Extension.Available -> MaterialSymbols.Rounded.Download to MR.strings.ext_install
            extension is Extension.Installed && extension.hasUpdate -> {
                MaterialSymbols.Rounded.Download to MR.strings.ext_update
            }
            else -> null
        }
        action?.let { (icon, label) ->
            IconButton(onClick = { onClickItemAction(extension) }) {
                Icon(imageVector = icon, contentDescription = stringResource(label))
            }
        }
    }
}
