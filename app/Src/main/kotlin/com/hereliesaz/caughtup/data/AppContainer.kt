package com.hereliesaz.caughtup.data

import android.content.Context

/**
 * Manual Dependency Injection because Dagger/Hilt is just bureaucratic overhead for a dragnet.
 */
interface AppContainer {
    val targetRepository: TargetRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val targetRepository: TargetRepository by lazy {
        OfflineTargetRepository(CaughtUpDatabase.getDatabase(context).targetDao())
    }
}
