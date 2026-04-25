package com.hereliesaz.caughtup.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The physical manifestation of your localized surveillance state.
 */
@Database(entities = [Target::class], version = 1, exportSchema = false)
abstract class CaughtUpDatabase : RoomDatabase() {
    abstract fun targetDao(): TargetDao

    companion object {
        @Volatile
        private var Instance: CaughtUpDatabase? = null

        fun getDatabase(context: Context): CaughtUpDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CaughtUpDatabase::class.java,
                    "caughtup_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { Instance = it }
            }
        }
    }
}

