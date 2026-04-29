package com.hereliesaz.cleanunderwear.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The physical manifestation of your localized surveillance state.
 */
@Database(entities = [Target::class], version = 5, exportSchema = false)
abstract class CleanUnderwearDatabase : RoomDatabase() {
    abstract fun targetDao(): TargetDao

    companion object {
        @Volatile
        private var Instance: CleanUnderwearDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE targets ADD COLUMN residence_info TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE targets ADD COLUMN lockup_url TEXT")
                database.execSQL("ALTER TABLE targets ADD COLUMN obituary_url TEXT")
                database.execSQL("ALTER TABLE targets ADD COLUMN check_frequency_hours INTEGER NOT NULL DEFAULT 24")
                database.execSQL("ALTER TABLE targets ADD COLUMN next_scheduled_check INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE targets ADD COLUMN last_status_change_timestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE targets ADD COLUMN last_verification_snippet TEXT")
            }
        }

        fun getDatabase(context: Context): CleanUnderwearDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CleanUnderwearDatabase::class.java,
                    "cleanunderwear_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { Instance = it }
            }
        }
    }
}

