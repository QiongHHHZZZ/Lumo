package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.screen.browse.StoreCandidate
import eu.kanade.presentation.more.settings.screen.browse.StoreSearchState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Finds a store on GitHub instead of asking for a URL.
 *
 * The result list shows only repositories that were checked and do contain a store index, so
 * everything here is addable. Star count and the repository's own description are shown because
 * they are what the user has to judge trust by — this dialog does not claim to.
 */
@Composable
fun ExtensionStoreSearchDialog(
    state: StoreSearchState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPick: (StoreCandidate) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var showQuery by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.extensionStoresScreen_searchGitHub)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                Text(
                    text = stringResource(MR.strings.extensionStoresScreen_searchGitHub_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(MR.strings.extensionStoresScreen_searchGitHub_query)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )

                when {
                    state.running -> {
                        // Probing each repository is the slow half, so show it rather than a
                        // spinner that lasts however long the slowest probe takes.
                        Text(
                            text = if (state.total > 0) {
                                stringResource(
                                    MR.strings.extensionStoresScreen_searchGitHub_progress,
                                    state.checked,
                                    state.total,
                                )
                            } else {
                                stringResource(MR.strings.extensionStoresScreen_searchGitHub_searching)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (state.total > 0) {
                            LinearProgressIndicator(
                                progress = { state.checked.toFloat() / state.total },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    state.results.isEmpty() -> {
                        // Only say "nothing found" once a search has actually run.
                        if (state.checked > 0) {
                            Text(
                                text = stringResource(MR.strings.extensionStoresScreen_searchGitHub_empty),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(state.results, key = { it.indexUrl }) { candidate ->
                                CandidateRow(candidate = candidate, onClick = { onPick(candidate) })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSearch, enabled = !state.running) {
                Text(text = stringResource(MR.strings.extensionStoresScreen_searchGitHub_search))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
private fun CandidateRow(candidate: StoreCandidate, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.padding.small),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = candidate.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "★ ${candidate.stars}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (candidate.description.isNotBlank()) {
            Text(
                text = candidate.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
