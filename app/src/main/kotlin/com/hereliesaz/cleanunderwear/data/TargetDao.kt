package com.hereliesaz.cleanunderwear.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * The grim ledger's interface.
 *
 * Most reads should go through the lite projection — the full Target row carries large URL +
 * verification-snippet text that, multiplied across thousands of rows, blows past Android's 2 MB
 * CursorWindow. Use the full-row queries only for bounded subsets (single id, ready+due, by
 * monitorability state).
 */
@Dao
interface TargetDao {

    // -- Lite list queries (projection — UI + dedup + triage) ----------------------------------

    @Query(
        "SELECT id, display_name, phone_number, area_code, status, last_scraped_timestamp, " +
            "source_account, residence_info, check_frequency_hours, next_scheduled_check, " +
            "last_status_change_timestamp, email, monitorability_state " +
            "FROM targets ORDER BY display_name ASC"
    )
    fun getAllTargetsLite(): Flow<List<TargetLite>>

    @Query(
        "SELECT id, display_name, phone_number, area_code, status, last_scraped_timestamp, " +
            "source_account, residence_info, check_frequency_hours, next_scheduled_check, " +
            "last_status_change_timestamp, email, monitorability_state " +
            "FROM targets ORDER BY display_name ASC"
    )
    suspend fun getAllTargetsLiteSnapshot(): List<TargetLite>

    // -- Bounded full-row queries -------------------------------------------------------------

    @Query("SELECT * FROM targets WHERE id = :id LIMIT 1")
    suspend fun getTargetById(id: Int): Target?

    @Query("SELECT * FROM targets WHERE id = :id LIMIT 1")
    fun observeTargetById(id: Int): Flow<Target?>

    @Query("SELECT * FROM targets WHERE id IN (:ids)")
    suspend fun getTargetsByIds(ids: List<Int>): List<Target>

    @Query(
        "SELECT * FROM targets " +
            "WHERE monitorability_state = :state " +
            "ORDER BY display_name ASC"
    )
    suspend fun getTargetsByMonitorability(state: MonitorabilityState): List<Target>

    @Query(
        "SELECT * FROM targets " +
            "WHERE monitorability_state = 'READY' " +
            "AND status != 'IGNORED' " +
            "AND next_scheduled_check <= :now " +
            "ORDER BY display_name ASC"
    )
    suspend fun getReadyDueTargets(now: Long): List<Target>

    @Query("SELECT * FROM targets WHERE status = :status ORDER BY display_name ASC")
    fun getTargetsByStatus(status: TargetStatus): Flow<List<Target>>

    // -- Targeted updates (avoid loading + rewriting full rows for tiny changes) ---------------

    @Query("UPDATE targets SET monitorability_state = :state WHERE id = :id")
    suspend fun updateMonitorabilityState(id: Int, state: MonitorabilityState)

    @Query(
        "UPDATE targets SET status = :status, last_status_change_timestamp = :timestamp " +
            "WHERE id = :id"
    )
    suspend fun updateStatusOnly(id: Int, status: TargetStatus, timestamp: Long)

    // -- Mutations ----------------------------------------------------------------------------

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

    // -- Legacy full-row reads -----------------------------------------------------------------
    // Avoid using this on production-sized data — it can exceed the CursorWindow. Kept for
    // the rare callers that genuinely need every column.
    @Query("SELECT * FROM targets ORDER BY display_name ASC")
    fun getAllTargets(): Flow<List<Target>>
}
