package app.luma.core.migration.migrations

import app.luma.core.migration.Migration
import app.luma.core.migration.MigrationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.SourcePreferences
import mihon.domain.extension.model.ContentWarning
import tachiyomi.core.common.preference.PreferenceStore

@Inject
@ContributesIntoSet(AppScope::class)
class ContentWarningMigration(
    private val preferenceStore: PreferenceStore,
    private val sourcePreferences: SourcePreferences,
) : Migration {
    override val version: Float = 30f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val showNsfwSource = preferenceStore.getBoolean("show_nsfw_source", true)
        if (!showNsfwSource.isSet()) return true

        if (!showNsfwSource.get()) {
            sourcePreferences.enabledContentWarnings.set(setOf(ContentWarning.SAFE))
        }
        showNsfwSource.delete()

        return true
    }
}
