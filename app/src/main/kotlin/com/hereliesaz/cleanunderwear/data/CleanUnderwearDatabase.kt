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
@Database(entities = [Target::class], version = 6, exportSchema = false)
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create the new table with correct nullability and the new 'email' column
                database.execSQL("""
                    CREATE TABLE targets_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        display_name TEXT NOT NULL,
                        phone_number TEXT,
                        area_code TEXT,
                        jurisdiction TEXT,
                        status TEXT NOT NULL,
                        last_scraped_timestamp INTEGER NOT NULL,
                        source_account TEXT,
                        residence_info TEXT,
                        lockup_url TEXT,
                        obituary_url TEXT,
                        check_frequency_hours INTEGER NOT NULL,
                        next_scheduled_check INTEGER NOT NULL,
                        last_status_change_timestamp INTEGER NOT NULL,
                        last_verification_snippet TEXT,
                        email TEXT
                    )
                """.trimIndent())

                // 2. Copy data from the old table
                database.execSQL("""
                    INSERT INTO targets_new (
                        id, display_name, phone_number, area_code, jurisdiction, status, 
                        last_scraped_timestamp, source_account, residence_info, lockup_url, 
                        obituary_url, check_frequency_hours, next_scheduled_check, 
                        last_status_change_timestamp, last_verification_snippet
                    )
                    SELECT 
                        id, display_name, phone_number, area_code, jurisdiction, status, 
                        last_scraped_timestamp, source_account, residence_info, lockup_url, 
                        obituary_url, check_frequency_hours, next_scheduled_check, 
                        last_status_change_timestamp, last_verification_snippet
                    FROM targets
                """.trimIndent())

                // 3. Drop the old table
                database.execSQL("DROP TABLE targets")

                // 4. Rename the new table
                database.execSQL("ALTER TABLE targets_new RENAME TO targets")
            }
        }

        fun getDatabase(context: Context): CleanUnderwearDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CleanUnderwearDatabase::class.java,
                    "cleanunderwear_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                .also { Instance = it }
            }
        }
    }
}

