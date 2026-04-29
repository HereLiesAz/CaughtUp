package com.hereliesaz.caughtup.data

import kotlinx.coroutines.flow.Flow

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
    override suspend fun insertTarget(target: Target) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            targetDao.insertTarget(target)
        }
    }
    override suspend fun insertTargets(targets: List<Target>) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            targetDao.insertTargets(targets)
        }
    }
    override suspend fun updateTarget(target: Target) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            targetDao.updateTarget(target)
        }
    }
    override suspend fun wipeSlateClean() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            targetDao.wipeSlateClean()
        }
    }
}

