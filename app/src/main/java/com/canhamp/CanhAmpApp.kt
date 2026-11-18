package com.canhamp

import android.app.Application
import com.canhamp.di.AppContainer

class CanhAmpApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
