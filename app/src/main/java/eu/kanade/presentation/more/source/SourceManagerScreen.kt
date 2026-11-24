package eu.kanade.presentation.more.source

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.ExtensionItemActions
import eu.kanade.presentation.browse.ExtensionItemContent
import eu.kanade.presentation.browse.ExtensionItemIcon
import eu.kanade.presentation.browse.components.BaseBrowseItem
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.more.source.SourceManagerViewModel.ExtensionCard
import eu.kanade.tachiyomi.ui.more.source.SourceManagerViewModel.State
import eu.kanade.tachiyomi.util.system.isPackageInstalled
import eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission
import kotlinx.coroutines.launch
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Extension management. Each extension keeps exactly the row content it had in Browse — icon, name,
 * store, language, version, status pills and actions — wrapped in a card so several of them read as
 * separate items rather than one continuous list.
 */
@Composable
fun SourceManagerScreen(
    state: State,
    showInstalled: Boolean,
    contentPadding: PaddingValues,
    onSelectTab: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onUpdateAll: () -> Unit,
    onInstall: (Extension.Available) -> Unit,
    onUpdate: (Extension.Installed) -> Unit,
    onUninstall: (Extension.Installed) -> Unit,
    onCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onTrust: (Extension.NotLoaded) -> Unit,
    onOpenStores: () -> Unit,
    onOpenDetails: (Extension.Loaded) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val installGranted = eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState(initialValue = true)
    val pagerState = rememberPagerState(initialPage = if (showInstalled) 0 else 1) { 2 }
    val scope = rememberCoroutineScope()

    // Privately-installed extensions can't be removed by the system installer, so the long-press
    // path asks first, exactly as the Browse list did before this screen existed.
    var privateExtensionToUninstall by remember { mutableStateOf<Extension.Installed?>(null) }

    // Keep the ViewModel in sync with whichever page the pager settles on, so the search filter and
    // the list stay on the same tab whether it was switched by tap or by swipe.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onSelectTab(page == 0)
        }
    }

    Column(modifier = Modifier.padding(contentPadding)) {
        if (state.installer?.requiresSystemPermission == true && !installGranted) {
            eu.kanade.presentation.components.WarningBanner(
                textRes = MR.strings.ext_permission_install_apps_warning,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.launchRequestPackageInstallsPermission() },
            )
        }

        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = {
                    Text(
                        text = stringResource(MR.strings.ext_installed) +
                            " (${state.installed.size})",
                    )
                },
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = {
                    Text(
                        text = stringResource(MR.strings.ext_available_tab) +
                            " (${state.available.size})",
                    )
                },
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val cards = if (page == 0) state.installed else state.available
            // PullRefresh must wrap the scrollable directly: with a plain Column between them the
            // drag never reaches it. The empty case still needs a scrollable child, otherwise there
            // is nothing to pull on, so it is rendered inside the same LazyColumn.
            PullRefresh(
                refreshing = state.isRefreshing,
                onRefresh = onRefresh,
                enabled = !state.isLoading,
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (cards.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(
                                installed = page == 0,
                                searching = !state.searchQuery.isNullOrEmpty(),
                                onOpenStores = onOpenStores,
                            )
                        }
                    } else {
                        if (page == 0 && state.updates > 0) {
                            item(key = "update-all") {
                                UpdateAllRow(count = state.updates, onUpdateAll = onUpdateAll)
                            }
                        }
                        items(cards, key = { it.extension.pkgName }) { card ->
                            ExtensionCardItem(
                                card = card,
                                onInstall = onInstall,
                                onUpdate = onUpdate,
                                onUninstall = onUninstall,
                                onLongPressUninstall = { installed ->
                                    if (context.isPackageInstalled(installed.pkgName)) {
                                        onUninstall(installed)
                                    } else {
                                        privateExtensionToUninstall = installed
                                    }
                                },
                                onCancel = onCancel,
                                onOpenWebView = onOpenWebView,
                                onTrust = onTrust,
                                onOpenDetails = onOpenDetails,
                            )
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }

    privateExtensionToUninstall?.let { extension ->
        ExtensionUninstallConfirmation(
            extensionName = extension.name,
            onClickConfirm = {
                onUninstall(extension)
                privateExtensionToUninstall = null
            },
            onDismissRequest = { privateExtensionToUninstall = null },
        )
    }
}

@Composable
private fun ExtensionUninstallConfirmation(
    extensionName: String,
    onClickConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.ext_confirm_remove)) },
        text = { Text(stringResource(MR.strings.remove_private_extension_message, extensionName)) },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    onClickConfirm()
                    onDismissRequest()
                },
            ) {
                Text(stringResource(MR.strings.ext_remove))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun UpdateAllRow(count: Int, onUpdateAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(MR.strings.ext_updates_pending) + " " + count,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        androidx.compose.material3.Button(onClick = onUpdateAll) {
            Text(stringResource(MR.strings.ext_update_all))
        }
    }
}

@Composable
private fun EmptyState(installed: Boolean, searching: Boolean, onOpenStores: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                when {
                    searching -> MR.strings.no_results_found
                    installed -> MR.strings.ext_not_loaded
                    else -> MR.strings.no_results_found
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.TextButton(onClick = onOpenStores) {
            Text(stringResource(MR.strings.extensionStores))
        }
    }
}

@Composable
private fun ExtensionCardItem(
    card: ExtensionCard,
    onInstall: (Extension.Available) -> Unit,
    onUpdate: (Extension.Installed) -> Unit,
    onUninstall: (Extension.Installed) -> Unit,
    onLongPressUninstall: (Extension.Installed) -> Unit,
    onCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onTrust: (Extension.NotLoaded) -> Unit,
    onOpenDetails: (Extension.Loaded) -> Unit,
) {
    val extension = card.extension

    val store: ExtensionStore? = when (extension) {
        is Extension.Available -> extension.store
        is Extension.Installed -> extension.store
    }

    // Plain list row, matching how Browse rendered these: no card, no divider, spacing only.
    BaseBrowseItem(
        modifier = Modifier,
        onClickItem = {
            when (extension) {
                is Extension.Available -> onInstall(extension)
                is Extension.Loaded -> onOpenDetails(extension)
                is Extension.NotLoaded -> Unit
            }
        },
        onLongClickItem = {
            when (extension) {
                is Extension.Available -> onInstall(extension)
                is Extension.Installed -> onLongPressUninstall(extension)
                is Extension.NotLoaded -> Unit
            }
        },
        icon = {
            ExtensionItemIcon(extension = extension, installStep = card.installStep)
        },
        action = {
            ExtensionItemActions(
                extension = extension,
                installStep = card.installStep,
                onClickItemCancel = onCancel,
                onClickItemAction = {
                    when (it) {
                        is Extension.Available -> onInstall(it)
                        is Extension.Installed -> if (it.hasUpdate) onUpdate(it)
                        is Extension.NotLoaded -> Unit
                    }
                },
                onClickItemSecondaryAction = {
                    when (it) {
                        is Extension.Available -> onOpenWebView(it)
                        is Extension.NotLoaded -> onTrust(it)
                        is Extension.Loaded -> onOpenDetails(it)
                    }
                },
            )
        },
    ) {
        ExtensionItemContent(
            extension = extension,
            installStep = card.installStep,
            store = store,
            modifier = Modifier.weight(1f),
        )
    }
}
