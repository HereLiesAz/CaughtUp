package com.hereliesaz.cleanunderwear.data

import org.junit.Test
import org.junit.Assert.assertEquals

class TargetDaoTest {

    @Test
    fun target_defaultStatus_isAtLarge() {
        val target = Target(
            displayName = "John Doe",
            phoneNumber = "555-1234",
            areaCode = "555"
        )
        assertEquals(TargetStatus.AT_LARGE, target.status)
    }
}
