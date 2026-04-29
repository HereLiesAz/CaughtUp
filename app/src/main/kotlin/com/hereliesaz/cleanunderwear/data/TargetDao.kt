package com.hereliesaz.cleanunderwear.data

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
    fun insertTarget(target: Target)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTargets(targets: List<Target>)

    @Update
    fun updateTarget(target: Target)

    @androidx.room.Delete
    fun deleteTarget(target: Target)

    @Query("DELETE FROM targets")
    fun wipeSlateClean()
}
