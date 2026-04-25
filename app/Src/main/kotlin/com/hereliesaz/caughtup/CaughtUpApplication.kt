package com.hereliesaz.caughtup

import android.app.Application
import com.hereliesaz.caughtup.data.AppContainer
import com.hereliesaz.caughtup.data.DefaultAppContainer

class CaughtUpApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}


