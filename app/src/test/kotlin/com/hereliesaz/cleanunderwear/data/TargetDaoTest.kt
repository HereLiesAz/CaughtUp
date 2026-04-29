package com.hereliesaz.cleanunderwear.data

import org.junit.Test
import org.junit.Assert.assertEquals

class TargetDaoTest {

    @Test
    fun target_defaultStatus_isMonitoring() {
        val target = Target(
            displayName = "John Doe",
            phoneNumber = "555-1234",
            areaCode = "555"
        )
        assertEquals(TargetStatus.MONITORING, target.status)
    }
}
