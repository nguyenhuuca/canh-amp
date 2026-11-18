package com.canhamp

import android.app.Application
import com.canhamp.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CanhAmpApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // B10: drop imported folders whose persisted grant was lost.
        appScope.launch {
            try {
                container.libraryRepository.pruneLostGrants()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                container.logger.w("CanhAmpApp", "grant prune failed: ${e.javaClass.simpleName}")
            }
        }
    }
}
