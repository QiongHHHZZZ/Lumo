package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.fragment.app.FragmentActivity
import app.luma.di.appGraph
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import mihon.domain.extension.model.ContentWarning
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Everything about how extensions behave: which installer is used, which content warnings are
 * allowed, and whether disabled sources stay hidden in library listings. These used to be split
 * across the Advanced and Browse settings screens, forcing two trips for one topic.
 */
object ExtensionSettingsScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.pref_category_extensions

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val basePreferences = remember { context.appGraph.basePreferences }
        val sourcePreferences = remember { context.appGraph.sourcePreferences }

        return listOf(
            getInstallerGroup(basePreferences),
            getContentWarningsGroup(sourcePreferences),
        )
    }

    @Composable
    private fun getInstallerGroup(basePreferences: BasePreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val extensionInstallerPref = basePreferences.extensionInstaller
        var shizukuMissing by rememberSaveable { mutableStateOf(false) }
        val trustExtension = remember { context.appGraph.trustExtension }

        if (shizukuMissing) {
            val dismiss = { shizukuMissing = false }
            AlertDialog(
                onDismissRequest = dismiss,
                title = { Text(text = stringResource(MR.strings.ext_installer_shizuku)) },
                text = { Text(text = stringResource(MR.strings.ext_installer_shizuku_unavailable_dialog)) },
                dismissButton = {
                    TextButton(onClick = dismiss) {
                        Text(text = stringResource(MR.strings.action_cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dismiss()
                            uriHandler.openUri("https://shizuku.rikka.app/download")
                        },
                    ) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.ext_installer_pref),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = extensionInstallerPref,
                    entries = extensionInstallerPref.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.ext_installer_pref),
                    onValueChanged = {
                        if (it == BasePreferences.ExtensionInstaller.SHIZUKU &&
                            !context.isShizukuInstalled
                        ) {
                            shizukuMissing = true
                            false
                        } else {
                            true
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.ext_revoke_trust),
                    onClick = { trustExtension.revokeAll() },
                ),
            ),
        )
    }

    @Composable
    private fun getContentWarningsGroup(
        sourcePreferences: eu.kanade.domain.source.service.SourcePreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_allowed_content_warnings),
            preferenceItems = listOf(
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = sourcePreferences.enabledContentWarnings,
                    entries = mapOf(
                        ContentWarning.SAFE to stringResource(MR.strings.ext_content_warning_safe),
                        ContentWarning.MIXED to stringResource(MR.strings.ext_content_warning_mixed),
                        ContentWarning.NSFW to stringResource(MR.strings.ext_content_warning_nsfw),
                    ),
                    title = stringResource(MR.strings.pref_allowed_content_warnings),
                    subtitleProvider = { value, entries ->
                        remember(value, entries) {
                            entries.filterKeys { it in value }.values.joinToString()
                        }
                            .takeUnless { it.isBlank() }
                            ?: stringResource(MR.strings.none)
                    },
                    onValueChanged = { newValue ->
                        val added = newValue - sourcePreferences.enabledContentWarnings.get()
                        if (added.any { it != ContentWarning.SAFE }) {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(MR.strings.pref_allowed_content_warnings),
                            )
                        } else {
                            true
                        }
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = sourcePreferences.applyContentWarningsToInstalled,
                    title = stringResource(MR.strings.pref_apply_content_warnings_to_installed),
                    subtitle = stringResource(MR.strings.pref_apply_content_warnings_to_installed_summary),
                    onValueChanged = { newValue ->
                        if (newValue) {
                            true
                        } else {
                            (context as FragmentActivity).authenticate(
                                title = context.stringResource(
                                    MR.strings.pref_apply_content_warnings_to_installed,
                                ),
                            )
                        }
                    },
                ),
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.content_warnings_info)),
            ),
        )
    }
}
