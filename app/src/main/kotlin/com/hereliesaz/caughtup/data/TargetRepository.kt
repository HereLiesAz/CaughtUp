package com.hereliesaz.caughtup.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * The middle manager of misery. Abstracting away the grim reality of SQL.
 */
interface TargetRepository {
    fun getAllTargets(): Flow<List<Target>>
    fun getTargetsByStatus(status: TargetStatus): Flow<List<Target>>
    suspend fun insertTarget(target: Target)
    suspend fun insertTargets(targets: List<Target>)
    suspend fun updateTarget(target: Target)
    suspend fun wipeSlateClean()
}

class OfflineTargetRepository(private val targetDao: TargetDao) : TargetRepository {
    override fun getAllTargets(): Flow<List<Target>> = targetDao.getAllTargets()
    override fun getTargetsByStatus(status: TargetStatus): Flow<List<Target>> = targetDao.getTargetsByStatus(status)
    override suspend fun insertTarget(target: Target) = withContext(Dispatchers.IO) { targetDao.insertTarget(target) }
    override suspend fun insertTargets(targets: List<Target>) = withContext(Dispatchers.IO) { targetDao.insertTargets(targets) }
    override suspend fun updateTarget(target: Target) = withContext(Dispatchers.IO) { targetDao.updateTarget(target) }
    override suspend fun wipeSlateClean() = withContext(Dispatchers.IO) { targetDao.wipeSlateClean() }
}
