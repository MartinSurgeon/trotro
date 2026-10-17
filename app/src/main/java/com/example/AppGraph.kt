package com.example

import android.content.Context
import com.example.data.CampaignDatabase
import com.example.data.CampaignRepository
import com.example.data.SettingsStore
import com.example.sms.SmsSenderService

/**
 * Process-wide singletons. The ViewModel and the SMS status receiver share one repository so
 * delivery reports that arrive while the app is closed are still recorded.
 */
object AppGraph {
    @Volatile private var repository: CampaignRepository? = null
    @Volatile private var settings: SettingsStore? = null
    @Volatile private var smsSender: SmsSenderService? = null

    fun repository(context: Context): CampaignRepository =
        repository ?: synchronized(this) {
            repository ?: CampaignRepository(
                CampaignDatabase.getInstance(context).campaignDao(),
                context.applicationContext
            ).also { repository = it }
        }

    fun settings(context: Context): SettingsStore =
        settings ?: synchronized(this) {
            settings ?: SettingsStore(context.applicationContext).also { settings = it }
        }

    fun smsSender(context: Context): SmsSenderService =
        smsSender ?: synchronized(this) {
            smsSender ?: SmsSenderService(
                context.applicationContext,
                repository(context),
                settings(context)
            ).also { smsSender = it }
        }
}
