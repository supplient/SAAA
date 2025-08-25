package com.example.strategicassetallocationassistant.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.strategicassetallocationassistant.domain.UpdateMarketDataUseCase
import com.example.strategicassetallocationassistant.domain.CheckTradingOpportunitiesUseCase
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
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
    private val updateMarketData: UpdateMarketDataUseCase,
    private val checkTradingOpportunities: CheckTradingOpportunitiesUseCase,
    private val repository: PortfolioRepository
) : CoroutineWorker(context, params) {

    // Fallback constructor for cases where HiltWorkerFactory is not used (e.g. mis-configuration during init)
    constructor(context: Context, params: WorkerParameters) : this(
        context,
        params,
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            UpdateMarketDataEntryPoint::class.java
        ).updateMarketData(),
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            UpdateMarketDataEntryPoint::class.java
        ).checkTradingOpportunities(),
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            UpdateMarketDataEntryPoint::class.java
        ).portfolioRepository()
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpdateMarketDataEntryPoint {
        fun updateMarketData(): UpdateMarketDataUseCase
        fun checkTradingOpportunities(): CheckTradingOpportunitiesUseCase
        fun portfolioRepository(): PortfolioRepository
    }

    override suspend fun doWork(): Result {
        return try {
            // 更新市场数据并获取成功/失败统计
            val stats = updateMarketData()
            // 通知用户市场数据已刷新
            NotificationHelper.notifyMarketDataUpdated(applicationContext, stats.success, stats.fail)
            // DEBUG 暂时先禁用自动交易机会检查
            // 检查交易机会并保存
            // val ops = checkTradingOpportunities()
            // if (ops.isNotEmpty()) {
            //     repository.insertTradingOpportunities(ops)
            //     NotificationHelper.notifyNewOpportunities(applicationContext, ops.size)
            // }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
