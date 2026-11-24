package eu.kanade.tachiyomi.core.security

import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.i18n.MR

@Inject
@SingleIn(AppScope::class)
class SecurityPreferences(
    preferenceStore: PreferenceStore,
) {

    val useAuthenticator: Preference<Boolean> = preferenceStore.getBoolean("use_biometric_lock", false)

    val lockAppAfter: Preference<Int> = preferenceStore.getInt("lock_app_after", 0)

    val secureScreen: Preference<SecureScreenMode> = preferenceStore.getEnum(
        "secure_screen_v2",
        SecureScreenMode.ALWAYS,
    )

    val hideNotificationContent: Preference<Boolean> = preferenceStore.getBoolean("hide_notification_content", false)

    /**
     * For app lock. Will be set when there is a pending timed lock.
     * Otherwise, this pref should be deleted.
     */
    val lastAppClosed: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("last_app_closed"),
        0,
    )

    /**
     * When to block screenshots and screen recording.
     *
     * There used to be a third choice, "only while incognito", tying the block to the incognito mode.
     * That mode has been removed, so the choice is now simply whether to block or not: keeping a
     * setting that can only ever mean "never" beside "always" would be a switch that does nothing.
     */
    enum class SecureScreenMode(val titleRes: StringResource) {
        ALWAYS(MR.strings.lock_always),
        NEVER(MR.strings.lock_never),
    }
}
