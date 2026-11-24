package eu.kanade.tachiyomi.ui.more.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.more.settings.screen.ExtensionSettingsScreen
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.presentation.more.source.SourceManagerScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionFilterScreen
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.Settings
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

/**
 * Extension management reached from Settings, so installing and uninstalling extensions lives next
 * to the rest of the configuration instead of inside Browse.
 */
data object ExtensionsSettingsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<SourceManagerViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val showInstalled by viewModel.showInstalled.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = searchQuery,
                    onChangeSearchQuery = viewModel::search,
                    navigateUp = navigator::pop,
                    titleContent = {
                        androidx.compose.material3.Text(
                            text = stringResource(MR.strings.label_extensions),
                        )
                    },
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_settings),
                                    icon = MaterialSymbols.Rounded.Settings,
                                    onClick = { navigator.push(ExtensionSettingsScreen) },
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_filter),
                                    icon = MaterialSymbols.Rounded.FilterList,
                                    onClick = { navigator.push(ExtensionFilterScreen()) },
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_addExtensionStore),
                                    icon = MaterialSymbols.Rounded.Add,
                                    onClick = { navigator.push(ExtensionStoresScreen()) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            if (state.isLoading) {
                LoadingScreen()
            } else {
                SourceManagerScreen(
                    state = state,
                    showInstalled = showInstalled,
                    contentPadding = contentPadding,
                    onSelectTab = viewModel::setInstalledTab,
                    onRefresh = viewModel::refresh,
                    onUpdateAll = viewModel::updateAll,
                    onInstall = viewModel::install,
                    onUpdate = viewModel::update,
                    onUninstall = viewModel::uninstall,
                    onCancel = viewModel::cancel,
                    onOpenWebView = { extension ->
                        extension.sources.firstOrNull()?.let {
                            navigator.push(
                                WebViewScreen(url = it.baseUrl, initialTitle = it.name, sourceId = it.id),
                            )
                        }
                    },
                    onTrust = viewModel::trust,
                    onOpenStores = { navigator.push(ExtensionStoresScreen()) },
                    onOpenDetails = { navigator.push(ExtensionDetailsScreen(it.pkgName)) },
                )
            }
        }
    }
}
