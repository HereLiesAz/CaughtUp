package com.hereliesaz.cleanunderwear.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetRepositoryTest {

    private val fakeDao = object : TargetDao {
        var targets = mutableListOf<Target>()

        override fun getAllTargets(): Flow<List<Target>> = flowOf(targets)
        override fun getTargetsByStatus(status: TargetStatus): Flow<List<Target>> = flowOf(targets.filter { it.status == status })
        override fun insertTarget(target: Target) { targets.add(target) }
        override fun insertTargets(newTargets: List<Target>) { targets.addAll(newTargets) }
        override fun updateTarget(target: Target) {
            targets.replaceAll { if (it.id == target.id) target else it }
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
