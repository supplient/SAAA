package com.example.strategicassetallocationassistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import android.util.Log
import java.util.concurrent.Executors
import javax.inject.Inject
import com.example.strategicassetallocationassistant.data.preferences.PreferencesRepository
import com.example.strategicassetallocationassistant.background.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerFactoryProvider {
        fun workerFactory(): HiltWorkerFactory
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var prefs: PreferencesRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        
        CoroutineScope(Dispatchers.Default).launch {
            val initialInterval = prefs.refreshIntervalMinutes.first()
            WorkScheduler.schedule(this@MainApplication, initialInterval)
            
            prefs.refreshIntervalMinutes.collect { interval ->
                // Reschedule work whenever the interval changes
                WorkScheduler.schedule(this@MainApplication, interval)
            }
        }
    }
}
