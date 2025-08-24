package com.example.strategicassetallocationassistant.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.strategicassetallocationassistant.domain.UpdateMarketDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors

@HiltWorker
class MarketDataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updateMarketData: UpdateMarketDataUseCase
) : CoroutineWorker(context, params) {

    // Fallback constructor for cases where HiltWorkerFactory is not used (e.g. mis-configuration during init)
    constructor(context: Context, params: WorkerParameters) : this(
        context,
        params,
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            UpdateMarketDataEntryPoint::class.java
        ).updateMarketData()
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpdateMarketDataEntryPoint {
        fun updateMarketData(): UpdateMarketDataUseCase
    }

    override suspend fun doWork(): Result {
        return try {
            updateMarketData()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
