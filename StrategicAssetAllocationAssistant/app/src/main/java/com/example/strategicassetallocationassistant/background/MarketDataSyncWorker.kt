package com.example.strategicassetallocationassistant.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.strategicassetallocationassistant.domain.UpdateMarketDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MarketDataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updateMarketData: UpdateMarketDataUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            updateMarketData()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
