package com.hereliesaz.caughtup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * The grim ledger's interface. 
 */
@Dao
interface TargetDao {
    @Query("SELECT * FROM targets ORDER BY display_name ASC")
    fun getAllTargets(): Flow<List<Target>>

    @Query("SELECT * FROM targets WHERE status = :status ORDER BY display_name ASC")
    fun getTargetsByStatus(status: TargetStatus): Flow<List<Target>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: Target)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<Target>)

    @Update
    suspend fun updateTarget(target: Target)

    @Query("DELETE FROM targets")
    suspend fun wipeSlateClean()
}

