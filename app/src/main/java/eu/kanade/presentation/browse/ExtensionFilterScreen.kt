package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionFilterState
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun ExtensionFilterScreen(
    navigateUp: () -> Unit,
    state: ExtensionFilterState.Success,
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    onClickToggle: (String) -> Unit,
    onClickToggleAll: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            SearchToolbar(
                searchQuery = searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
                navigateUp = navigateUp,
                titleContent = {
                    // The screen toggles which source languages are enabled, so naming it after the
                    // extension list was misleading.
                    Text(text = stringResource(MR.strings.ext_filter_languages))
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onClickToggleAll(true) }) {
                    Text(stringResource(MR.strings.action_enable_all))
                }
                TextButton(onClick = { onClickToggleAll(false) }) {
                    Text(stringResource(MR.strings.action_disable_all))
                }
            }
            HorizontalDivider()

            if (state.isEmpty) {
                EmptyScreen(stringRes = MR.strings.empty_screen)
            } else {
                ExtensionFilterContent(
                    state = state,
                    onClickLang = onClickToggle,
                )
            }
        }
    }
}

@Composable
private fun ExtensionFilterContent(
    state: ExtensionFilterState.Success,
    onClickLang: (String) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn {
        items(state.languages) { language ->
            SwitchPreferenceWidget(
                modifier = Modifier.animateItem(),
                title = LocaleHelper.getSourceDisplayName(language, context),
                checked = language in state.enabledLanguages,
                onCheckedChanged = { onClickLang(language) },
            )
        }
    }
}
