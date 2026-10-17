package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContactEntity::class,
        SenderIdEntity::class,
        OrganisationProfileEntity::class,
        MessageBroadcastEntity::class,
        RecipientDeliveryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CampaignDatabase : RoomDatabase() {
    abstract fun campaignDao(): CampaignDao

    companion object {
        @Volatile
        private var INSTANCE: CampaignDatabase? = null

        fun getInstance(context: Context): CampaignDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CampaignDatabase::class.java,
                    "campaign_sms_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
