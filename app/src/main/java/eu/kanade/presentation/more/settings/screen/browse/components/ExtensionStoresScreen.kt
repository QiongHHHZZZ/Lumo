package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoreScreenState
import mihon.domain.extension.model.ExtensionStore
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Refresh
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

/**
 * 插件仓库列表。
 *
 * ## 为什么是右下角按钮
 *
 * 顶部原来并排放着「搜索 GitHub」和「添加仓库」两个大按钮，占掉一整行，
 * 每次进这个页面第一眼看到的是按钮而不是仓库列表。而这两个动作是低频的：
 * 加过一次仓库之后，几乎不会再来点它们。
 *
 * 现在收成右下角一个按钮。点开后再选是手动填地址，还是去 GitHub 搜。
 * 这样列表本身占满屏幕，动作仍然找得到——悬浮按钮是安卓上「新增」的常规位置，
 * 不需要学。
 */
@Composable
fun ExtensionStoresScreen(
    state: ExtensionStoreScreenState.Success,
    onClickCreate: () -> Unit,
    onClickSearch: () -> Unit,
    onCopy: (ExtensionStore) -> Unit,
    onOpenWebsite: (ExtensionStore) -> Unit,
    onOpenDiscord: (ExtensionStore) -> Unit,
    onClickDelete: (ExtensionStore) -> Unit,
    onClickRefresh: () -> Unit,
    navigateUp: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    var showAddOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                navigateUp = navigateUp,
                title = stringResource(MR.strings.extensionStores),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onClickRefresh) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Refresh,
                            contentDescription = stringResource(resource = MR.strings.action_webview_refresh),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddOptions = true },
                icon = {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_title)) },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (state.isEmpty) {
                EmptyScreen(
                    MR.strings.extensionStoresScreen_emptyLabel,
                )
                return@Column
            }

            ExtensionStoresContent(
                repos = state.stores,
                lazyListState = lazyListState,
                paddingValues = PaddingValues(
                    start = MaterialTheme.padding.medium,
                    end = MaterialTheme.padding.medium,
                    top = MaterialTheme.padding.small,
                ),
                onCopy = onCopy,
                onOpenWebsite = onOpenWebsite,
                onOpenDiscord = onOpenDiscord,
                onClickDelete = onClickDelete,
            )
        }
    }

    if (showAddOptions) {
        AlertDialog(
            onDismissRequest = { showAddOptions = false },
            title = { Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_title)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_title))
                        },
                        supportingContent = {
                            Text(text = stringResource(MR.strings.extensionStoresScreen_addStoreInput_inputLabel))
                        },
                        modifier = Modifier.clickable {
                            showAddOptions = false
                            onClickCreate()
                        },
                    )
                    ListItem(
                        headlineContent = {
                            Text(text = stringResource(MR.strings.extensionStoresScreen_searchGitHub))
                        },
                        modifier = Modifier.clickable {
                            showAddOptions = false
                            onClickSearch()
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddOptions = false }) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}
