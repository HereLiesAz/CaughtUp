package com.hereliesaz.cleanunderwear.data

import androidx.paging.PagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * The middle manager of misery. Abstracting away the grim reality of SQL.
 */
interface TargetRepository {

    // Lite reads — what the UI list screen and the dedup/triage phases use.
    fun getAllTargetsLite(): Flow<List<TargetLite>>
    fun searchTargets(
        query: String,
        showIgnored: Boolean,
        googleF: Boolean?,
        metaF: Boolean?,
        appleF: Boolean?,
        deviceF: Boolean?,
        namelessF: Boolean?,
        emailOnlyF: Boolean?,
        hasEmailF: Boolean?,
        hasAddressF: Boolean?,
        pendingEnrichF: Boolean?,
        sort: String
    ): PagingSource<Int, TargetLite>

    suspend fun getTargetsPaged(limit: Int, offset: Int): List<TargetLite>
    suspend fun getAllTargetSourceInfo(): List<TargetSourceInfo>

    // Bounded full-row reads.
    suspend fun getTargetById(id: Int): Target?
    fun observeTargetById(id: Int): Flow<Target?>
    suspend fun getTargetsByIds(ids: List<Int>): List<Target>
    suspend fun getTargetsByMonitorability(state: MonitorabilityState): List<Target>
    suspend fun getReadyDueTargets(now: Long): List<Target>

    // Legacy — only safe for small datasets.
    fun getAllTargets(): Flow<List<Target>>
    fun getTargetsByStatus(status: TargetStatus): Flow<List<Target>>

    // Targeted updates.
    suspend fun updateMonitorabilityState(id: Int, state: MonitorabilityState)
    suspend fun updateMonitorabilityStateBatch(ids: List<Int>, state: MonitorabilityState)
    suspend fun updateStatusOnly(id: Int, status: TargetStatus, timestamp: Long)
    suspend fun updateUrls(id: Int, lockupUrl: String?, obituaryUrl: String?)

    // Mutations.
    suspend fun insertTarget(target: Target)
    suspend fun insertTargets(targets: List<Target>)
    suspend fun updateTarget(target: Target)
    suspend fun deleteTarget(target: Target)
    suspend fun wipeSlateClean()
}

class OfflineTargetRepository(private val targetDao: TargetDao) : TargetRepository {

    override fun getAllTargetsLite(): Flow<List<TargetLite>> = targetDao.getAllTargetsLite()
    
    override fun searchTargets(
        query: String,
        showIgnored: Boolean,
        googleF: Boolean?,
        metaF: Boolean?,
        appleF: Boolean?,
        deviceF: Boolean?,
        namelessF: Boolean?,
        emailOnlyF: Boolean?,
        hasEmailF: Boolean?,
        hasAddressF: Boolean?,
        pendingEnrichF: Boolean?,
        sort: String
    ): PagingSource<Int, TargetLite> = targetDao.searchTargets(
        query, showIgnored, googleF, metaF, appleF, deviceF,
        namelessF, emailOnlyF, hasEmailF, hasAddressF,
        pendingEnrichF, sort
    )

    override suspend fun getTargetsPaged(limit: Int, offset: Int): List<TargetLite> =
        withContext(Dispatchers.IO) { targetDao.getTargetsPaged(limit, offset) }

    override suspend fun getAllTargetSourceInfo(): List<TargetSourceInfo> =
        withContext(Dispatchers.IO) { targetDao.getAllTargetSourceInfo() }

    override suspend fun getTargetById(id: Int): Target? =
        withContext(Dispatchers.IO) { targetDao.getTargetById(id) }

    override fun observeTargetById(id: Int): Flow<Target?> = targetDao.observeTargetById(id)

    override suspend fun getTargetsByIds(ids: List<Int>): List<Target> =
        withContext(Dispatchers.IO) { targetDao.getTargetsByIds(ids) }

    override suspend fun getTargetsByMonitorability(state: MonitorabilityState): List<Target> =
        withContext(Dispatchers.IO) { targetDao.getTargetsByMonitorability(state) }

    override suspend fun getReadyDueTargets(now: Long): List<Target> =
        withContext(Dispatchers.IO) { targetDao.getReadyDueTargets(now) }

    override fun getAllTargets(): Flow<List<Target>> = targetDao.getAllTargets()
    override fun getTargetsByStatus(status: TargetStatus): Flow<List<Target>> =
        targetDao.getTargetsByStatus(status)

    override suspend fun updateMonitorabilityState(id: Int, state: MonitorabilityState) =
        withContext(Dispatchers.IO) { targetDao.updateMonitorabilityState(id, state) }

    override suspend fun updateMonitorabilityStateBatch(ids: List<Int>, state: MonitorabilityState) =
        withContext(Dispatchers.IO) { targetDao.updateMonitorabilityStateBatch(ids, state) }

    override suspend fun updateStatusOnly(id: Int, status: TargetStatus, timestamp: Long) =
        withContext(Dispatchers.IO) { targetDao.updateStatusOnly(id, status, timestamp) }

    override suspend fun updateUrls(id: Int, lockupUrl: String?, obituaryUrl: String?) =
        withContext(Dispatchers.IO) { targetDao.updateUrls(id, lockupUrl, obituaryUrl) }

    override suspend fun insertTarget(target: Target) =
        withContext(Dispatchers.IO) { targetDao.insertTarget(target) }
    override suspend fun insertTargets(targets: List<Target>) =
        withContext(Dispatchers.IO) { targetDao.insertTargets(targets) }
    override suspend fun updateTarget(target: Target) =
        withContext(Dispatchers.IO) { targetDao.updateTarget(target) }
    override suspend fun deleteTarget(target: Target) =
        withContext(Dispatchers.IO) { targetDao.deleteTarget(target) }
    override suspend fun wipeSlateClean() =
        withContext(Dispatchers.IO) { targetDao.wipeSlateClean() }
}
