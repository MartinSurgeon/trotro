package com.example.data

import android.content.Context
import com.example.BuildConfig

/**
 * Small key-value settings that do not belong in the Room schema.
 *
 * The SMSOnlineGH API key entered by the user lives here. The file is excluded from cloud
 * backup (see res/xml/backup_rules.xml and data_extraction_rules.xml).
 */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var smsOnlineGhApiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var hasCompletedWelcome: Boolean
        get() = prefs.getBoolean(KEY_WELCOME_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_WELCOME_DONE, value).apply()

    /**
     * Messages whose failures the user chose to leave as they are ("Mark as done"), so they stop
     * showing under "Needs attention". Kept here rather than in Room to avoid a schema change.
     */
    var dismissedAttentionIds: Set<Long>
        get() = prefs.getStringSet(KEY_DISMISSED, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        set(value) = prefs.edit().putStringSet(KEY_DISMISSED, value.map { it.toString() }.toSet()).apply()

    /** Key typed in the app, falling back to the one baked in at build time (if any). */
    fun effectiveApiKey(): String? {
        val userKey = smsOnlineGhApiKey.trim()
        if (isRealKey(userKey)) return userKey
        val buildKey = BuildConfig.SMSONLINEGH_API_KEY.trim()
        return buildKey.takeIf { isRealKey(it) }
    }

    fun isUsingBuildKey(): Boolean = !isRealKey(smsOnlineGhApiKey) && effectiveApiKey() != null

    private fun isRealKey(key: String): Boolean =
        key.isNotEmpty() && key != "YOUR_SMSONLINEGH_API_KEY" && key != "MY_SMSONLINEGH_API_KEY"

    companion object {
        const val FILE_NAME = "dawuro_settings"
        private const val KEY_API_KEY = "smsonlinegh_api_key"
        private const val KEY_WELCOME_DONE = "welcome_done"
        private const val KEY_DISMISSED = "dismissed_attention_ids"
    }
}
