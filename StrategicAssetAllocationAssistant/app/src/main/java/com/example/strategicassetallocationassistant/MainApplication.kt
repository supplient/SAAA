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

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerFactoryProvider {
        fun workerFactory(): HiltWorkerFactory
    }

    override val workManagerConfiguration: Configuration
        get() {
            val factory = EntryPointAccessors.fromApplication(this, WorkerFactoryProvider::class.java).workerFactory()
            return Configuration.Builder()
                .setWorkerFactory(factory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        scheduleMarketDataSync()
    }

    private fun scheduleMarketDataSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
       val workRequest = PeriodicWorkRequestBuilder<com.example.strategicassetallocationassistant.background.MarketDataSyncWorker>(
           15, TimeUnit.MINUTES
       )
           .setConstraints(constraints)
           .build()
       val operation = WorkManager.getInstance(this).enqueueUniquePeriodicWork(
           "MarketDataSync",
           androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
           workRequest
       )

       // Listen for the result of the enqueue operation and log it
       operation.result.addListener(
           {
               try {
                   val state = operation.result.get()
                   Log.d("MarketDataSync", "Enqueue result for ${workRequest.id}: $state")
               } catch (e: Exception) {
                   Log.e("MarketDataSync", "Failed to enqueue ${workRequest.id}", e)
               }
           },
           Executors.newSingleThreadExecutor()
       )
    }
}
