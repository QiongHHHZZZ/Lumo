package app.luma.core.migration.migrations

import app.luma.core.migration.Migration
import app.luma.core.migration.MigrationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import mihon.core.common.FeatureFlags
import kotlin.uuid.ExperimentalUuidApi

@Inject
@ContributesIntoSet(AppScope::class)
class InstallationIdMigration(
    private val basePreferences: BasePreferences,
) : Migration {
    override val version: Float = Migration.ALWAYS

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val installationId = basePreferences.installationId
        if (!installationId.isSet()) installationId.set(FeatureFlags.newInstallationId())
        return true
    }
}
