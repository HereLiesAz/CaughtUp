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
@Database(entities = [Target::class], version = 2, exportSchema = false)
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

        fun getDatabase(context: Context): CleanUnderwearDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CleanUnderwearDatabase::class.java,
                    "cleanunderwear_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { Instance = it }
            }
        }
    }
}

