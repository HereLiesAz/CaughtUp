package com.hereliesaz.cleanunderwear.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetRepositoryTest {

    private val fakeDao = object : TargetDao {
        var targets = mutableListOf<Target>()

        private fun toLite(t: Target): TargetLite = TargetLite(
            id = t.id,
            displayName = t.displayName,
            phoneNumber = t.phoneNumber,
            areaCode = t.areaCode,
            status = t.status,
            lastScrapedTimestamp = t.lastScrapedTimestamp,
            sourceAccount = t.sourceAccount,
            residenceInfo = t.residenceInfo,
            checkFrequencyHours = t.checkFrequencyHours,
            nextScheduledCheck = t.nextScheduledCheck,
            lastStatusChangeTimestamp = t.lastStatusChangeTimestamp,
            email = t.email,
            monitorabilityState = t.monitorabilityState
        )

        override fun getAllTargetsLite(): Flow<List<TargetLite>> = flowOf(targets.map(::toLite))
        override suspend fun getAllTargetsLiteSnapshot(): List<TargetLite> = targets.map(::toLite)
        override suspend fun getTargetById(id: Int): Target? = targets.firstOrNull { it.id == id }
        override fun observeTargetById(id: Int): Flow<Target?> =
            flowOf(targets.firstOrNull { it.id == id })
        override suspend fun getTargetsByIds(ids: List<Int>): List<Target> =
            targets.filter { it.id in ids }
        override suspend fun getTargetsByMonitorability(state: MonitorabilityState): List<Target> =
            targets.filter { it.monitorabilityState == state }
        override suspend fun getReadyDueTargets(now: Long): List<Target> = targets.filter {
            it.monitorabilityState == MonitorabilityState.READY &&
                it.status != TargetStatus.IGNORED &&
                it.nextScheduledCheck <= now
        }
        override fun getAllTargets(): Flow<List<Target>> = flowOf(targets)
        override fun getTargetsByStatus(status: TargetStatus): Flow<List<Target>> =
            flowOf(targets.filter { it.status == status })

        override suspend fun updateMonitorabilityState(id: Int, state: MonitorabilityState) {
            targets.replaceAll { if (it.id == id) it.copy(monitorabilityState = state) else it }
        }
        override suspend fun updateStatusOnly(id: Int, status: TargetStatus, timestamp: Long) {
            targets.replaceAll {
                if (it.id == id) it.copy(status = status, lastStatusChangeTimestamp = timestamp)
                else it
            }
        }

        override fun insertTarget(target: Target) { targets.add(target) }
        override fun insertTargets(newTargets: List<Target>) { targets.addAll(newTargets) }
        override fun updateTarget(target: Target) {
            targets.replaceAll { if (it.id == target.id) target else it }
        }
        override fun deleteTarget(target: Target) {
            targets.removeAll { it.id == target.id }
        }
        override fun wipeSlateClean() { targets.clear() }
    }

    private val repository = OfflineTargetRepository(fakeDao)

    @Test
    fun repository_insertAndGetAll_worksCorrectly() = runTest {
        val target = Target(id = 1, displayName = "John Doe", phoneNumber = "123", areaCode = "123")
        repository.insertTarget(target)

        repository.getAllTargets().collect { list ->
            assertEquals(1, list.size)
            assertEquals("John Doe", list[0].displayName)
        }
    }

    @Test
    fun repository_update_worksCorrectly() = runTest {
        val target = Target(id = 1, displayName = "John Doe", phoneNumber = "123", areaCode = "123")
        repository.insertTarget(target)

        val updatedTarget = target.copy(status = TargetStatus.INCARCERATED)
        repository.updateTarget(updatedTarget)

        repository.getTargetsByStatus(TargetStatus.INCARCERATED).collect { list ->
            assertEquals(1, list.size)
            assertEquals(TargetStatus.INCARCERATED, list[0].status)
        }
    }
}
