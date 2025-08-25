package com.example.strategicassetallocationassistant.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val UNIQUE_NAME = "MarketDataSync"

    fun schedule(context: Context, intervalMinutes: Long) {
        // If the user chooses to disable background refresh (interval <= 0),
        // cancel any previously scheduled work and exit early.
        if (intervalMinutes <= 0) {
            cancel(context)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<MarketDataSyncWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Cancel the existing periodic work.
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}
