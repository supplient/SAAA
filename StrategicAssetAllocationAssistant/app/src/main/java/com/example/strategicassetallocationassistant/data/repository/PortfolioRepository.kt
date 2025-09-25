package com.example.strategicassetallocationassistant.data.repository

import androidx.room.withTransaction
import com.example.strategicassetallocationassistant.Asset
import com.example.strategicassetallocationassistant.Portfolio
import com.example.strategicassetallocationassistant.TradeType
import com.example.strategicassetallocationassistant.TradingOpportunity
import com.example.strategicassetallocationassistant.Transaction
import java.math.BigDecimal
import com.example.strategicassetallocationassistant.data.database.AppDatabase
import com.example.strategicassetallocationassistant.data.database.dao.AssetDao
import com.example.strategicassetallocationassistant.data.database.dao.PortfolioDao
import com.example.strategicassetallocationassistant.data.database.dao.TradingOpportunityDao
import com.example.strategicassetallocationassistant.data.database.dao.TransactionDao
import com.example.strategicassetallocationassistant.data.database.entities.AssetEntity
import com.example.strategicassetallocationassistant.data.database.entities.PortfolioEntity
import com.example.strategicassetallocationassistant.data.database.entities.TradingOpportunityEntity
import com.example.strategicassetallocationassistant.data.database.entities.TransactionEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * Repository responsible for interacting with Room database and converting
 * entities into domain models that the rest of the application can use.
 */
@Singleton
class PortfolioRepository @Inject constructor(
    private val assetDao: AssetDao,
    private val assetAnalysisDao: com.example.strategicassetallocationassistant.data.database.dao.AssetAnalysisDao,
    private val portfolioDao: PortfolioDao,
    private val transactionDao: TransactionDao,
    private val db: AppDatabase
) {
    private val tradingOpportunityDao: TradingOpportunityDao = db.tradingOpportunityDao()

    /* ---------------------------- 导入/导出 ---------------------------- */

    @Serializable
    private data class PortfolioBackup(
        val portfolio: PortfolioEntity,
        val assets: List<AssetEntity>
    )

    suspend fun exportDataToJson(): String {
        val portfolio = portfolioDao.getPortfolioSuspend() ?: return ""
        val assets = assetDao.getAllAssets().first()
        val backupData = PortfolioBackup(portfolio, assets)
        return Json.encodeToString(backupData)
    }

    suspend fun importDataFromJson(json: String) {
        val backupData = Json.decodeFromString<PortfolioBackup>(json)
        db.withTransaction {
            // 清空所有数据
            assetDao.deleteAllAssets()
            portfolioDao.deletePortfolio()
            transactionDao.deleteAllTransactions()
            tradingOpportunityDao.deleteAll()
            
            // 导入新数据
            assetDao.insertAssets(backupData.assets)
            portfolioDao.insertPortfolio(backupData.portfolio)
        }
    }


    /* ---------------------------- 读取数据 ---------------------------- */

    /**
     * Observe all [Asset]s in the database. Whenever the underlying data changes,
     * the emitted list will update automatically.
     */
    val assetsFlow: Flow<List<Asset>> = assetDao.getAllAssets().map { list ->
        list.map { it.toDomain() }
    }

    /**
     * Observe the full [Portfolio] (assets + cash) in one convenient stream.
     * 步骤6: 切换到BigDecimal版本
     */
    val portfolioFlow: Flow<Portfolio> = kotlinx.coroutines.flow.combine(
        portfolioDao.getPortfolio(),
        assetsFlow
    ) { entity, assets ->
        Portfolio(
            assets = assets,
            // 步骤6: 优先使用BigDecimal，向后兼容Double
            cash = entity?.getCashValue()?.toDouble() ?: 0.0,
            cashDecimal = entity?.getCashValue(),
            note = entity?.note,
            overallRiskFactor = entity?.overallRiskFactor,
            overallRiskFactorLog = entity?.overallRiskFactorLog
        )
    }

    /**
     * Observe all [Transaction]s in the database.
     */
    val transactionsFlow: Flow<List<Transaction>> = transactionDao.getAllTransactions().map { list ->
        list.map { it.toDomain() }
    }

    /**
     * Observe all TradingOpportunities.
     */
    val tradingOpportunitiesFlow: Flow<List<com.example.strategicassetallocationassistant.TradingOpportunity>> =
        tradingOpportunityDao.getAll().map { list -> list.map { it.toDomain() } }

    /**
     * Suspended version that returns the latest [Portfolio] once.
     */
    suspend fun getPortfolioOnce(): Portfolio {
        val assets = assetDao.getAllAssets().first()
        val entity = portfolioDao.getPortfolioSuspend()
        val cash = entity?.cash ?: 0.0
        val note = entity?.note
        return Portfolio(
            assets = assets.map { it.toDomain() }, 
            cash = cash,
            cashDecimal = entity?.getCashValue(),
            note = note
        )
    }

    /* ---------------------------- 写入数据 ---------------------------- */

    suspend fun insertAsset(asset: Asset) {
        assetDao.insertAsset(asset.toEntity())
    }

    suspend fun updateAsset(asset: Asset) {
        assetDao.updateAsset(asset.toEntity())
    }

    suspend fun deleteAsset(asset: Asset) {
        assetDao.deleteAssetById(asset.id.toString())
    }

    suspend fun getAssetById(id: java.util.UUID): Asset? {
        return assetDao.getAssetById(id.toString())?.toDomain()
    }

    suspend fun updateCash(cash: Double) {
        // 步骤6: 向后兼容的Double版本，内部转换为BigDecimal
        updateCashDecimal(BigDecimal.valueOf(cash))
    }

    /** 
     * 步骤6: BigDecimal版本的现金更新，提供精确计算
     */
    suspend fun updateCashDecimal(cashDecimal: BigDecimal) {
        val current = portfolioDao.getPortfolioSuspend()
        if (current == null) {
            // Insert new record if not exist
            portfolioDao.insertPortfolio(PortfolioEntity.createWithDecimal(cashDecimal = cashDecimal))
        } else {
            // 更新时保持BigDecimal和Double同步
            portfolioDao.updateCash(cashDecimal.toDouble())
        }
    }

    suspend fun updateNote(note: String?) {
        val current = portfolioDao.getPortfolioSuspend()
        if (current == null) {
            // insert new record with note
            portfolioDao.insertPortfolio(PortfolioEntity.create(cash = 0.0, note = note))
        } else {
            portfolioDao.updateNote(note)
        }
    }

    /* --------------------- Overall Risk Factor --------------------- */
    suspend fun updateOverallRiskFactor(f: Double) {
        portfolioDao.updateOverallRiskFactor(f)
    }

    suspend fun updateOverallRiskFactorWithLog(f: Double, log: String) {
        portfolioDao.updateOverallRiskFactorWithLog(f, log)
    }

    /* --------------------- AssetAnalysis 相关方法 --------------------- */
    
    /**
     * 观察所有资产分析数据
     */
    val assetAnalysisFlow: Flow<List<com.example.strategicassetallocationassistant.AssetAnalysis>> = 
        assetAnalysisDao.getAllAssetAnalysis().map { list ->
            list.map { it.toDomain() }
        }

    /**
     * 根据资产ID获取分析数据
     */
    suspend fun getAssetAnalysisById(assetId: UUID): com.example.strategicassetallocationassistant.AssetAnalysis? {
        return assetAnalysisDao.getAssetAnalysisById(assetId.toString())?.toDomain()
    }

    /**
     * 插入或更新资产分析数据
     */
    suspend fun upsertAssetAnalysis(analysis: com.example.strategicassetallocationassistant.AssetAnalysis) {
        assetAnalysisDao.insertAssetAnalysis(analysis.toEntity())
    }

    /**
     * 更新资产的市场数据（波动率和七日收益率）
     */
    suspend fun updateAssetMarketData(assetId: UUID, volatility: Double?, sevenDayReturn: Double?) {
        val existing = assetAnalysisDao.getAssetAnalysisById(assetId.toString())?.toDomain()
            ?: com.example.strategicassetallocationassistant.AssetAnalysis(assetId = assetId)
        
        val updated = existing.copy(
            volatility = volatility,
            sevenDayReturn = sevenDayReturn,
            lastUpdateTime = java.time.LocalDateTime.now()
        )
        upsertAssetAnalysis(updated)
    }

    /**
     * 更新资产的买入因子和计算过程日志（包含中间计算结果）
     */
    suspend fun updateAssetBuyFactorWithLog(
        assetId: UUID,
        buyFactor: Double,
        buyFactorLog: String,
        relativeOffset: Double,
        offsetFactor: Double,
        drawdownFactor: Double,
        preVolatilityBuyFactor: Double
    ) {
        val existing = assetAnalysisDao.getAssetAnalysisById(assetId.toString())?.toDomain()
            ?: com.example.strategicassetallocationassistant.AssetAnalysis(assetId = assetId)

        val updated = existing.copy(
            buyFactor = buyFactor,
            buyFactorLog = buyFactorLog,
            relativeOffset = relativeOffset,
            offsetFactor = offsetFactor,
            drawdownFactor = drawdownFactor,
            preVolatilityBuyFactor = preVolatilityBuyFactor
        )
        upsertAssetAnalysis(updated)
    }

    /**
     * 更新资产的卖出阈值和计算过程日志（包含中间计算结果）
     */
    suspend fun updateAssetSellThresholdWithLog(
        assetId: UUID,
        sellThreshold: Double,
        sellThresholdLog: String,
        assetRisk: Double
    ) {
        val existing = assetAnalysisDao.getAssetAnalysisById(assetId.toString())?.toDomain()
            ?: com.example.strategicassetallocationassistant.AssetAnalysis(assetId = assetId)

        val updated = existing.copy(
            sellThreshold = sellThreshold,
            sellThresholdLog = sellThresholdLog,
            assetRisk = assetRisk
        )
        upsertAssetAnalysis(updated)
    }

    /**
     * 添加一条交易记录，同时原子更新资产份额和现金。
     * 步骤6: 切换到BigDecimal精确计算
     */
    suspend fun addTransaction(tx: Transaction) {
        db.withTransaction {
            // 插入交易
            transactionDao.insertTransaction(tx.toEntity())

            // 更新资产 shares - 使用BigDecimal精确计算
            tx.assetId?.let { aid ->
                val asset = assetDao.getAssetById(aid.toString()) ?: return@withTransaction
                val currentSharesDecimal = asset.getSharesValue() ?: BigDecimal.ZERO
                val deltaSharesDecimal = tx.getSharesValue()
                val newSharesDecimal = if (tx.type == TradeType.BUY) {
                    currentSharesDecimal.add(deltaSharesDecimal)
                } else {
                    currentSharesDecimal.subtract(deltaSharesDecimal)
                }
                
                // 更新资产，同时维护Double和BigDecimal同步
                val updated = asset.withSyncedFields().copy(
                    shares = newSharesDecimal.toDouble(),
                    sharesDecimal = newSharesDecimal
                )
                assetDao.updateAsset(updated)
            }

            // 更新现金 - 使用BigDecimal精确计算
            val cashDeltaDecimal = tx.getAmountValue()
            val finalCashDelta = if (tx.type == TradeType.BUY) {
                cashDeltaDecimal.negate()
            } else {
                cashDeltaDecimal
            }
            
            val currentPortfolio = portfolioDao.getPortfolioSuspend()
            if (currentPortfolio == null) {
                portfolioDao.insertPortfolio(PortfolioEntity.createWithDecimal(cashDecimal = finalCashDelta))
            } else {
                val currentCashDecimal = currentPortfolio.getCashValue()
                val newCashDecimal = currentCashDecimal.add(finalCashDelta)
                portfolioDao.updateCash(newCashDecimal.toDouble())
            }
        }
    }

    suspend fun getTransactionById(id: java.util.UUID): Transaction? {
        return transactionDao.getAllTransactions().first().firstOrNull { java.util.UUID.fromString(it.id) == id }?.toDomain()
    }

    /**
     * 步骤6: 切换到BigDecimal精确计算
     */
    suspend fun deleteTransaction(tx: Transaction) {
        db.withTransaction {
            // 先获取数据库中的最新交易记录，以免调用方传入的是过时对象
            val entity = transactionDao.getTransactionById(tx.id.toString()) ?: return@withTransaction

            // 还原资产 shares - 使用BigDecimal精确计算
            entity.assetId?.let { aid ->
                val asset = assetDao.getAssetById(aid) ?: return@withTransaction
                val currentSharesDecimal = asset.getSharesValue() ?: BigDecimal.ZERO
                val deltaSharesDecimal = entity.getSharesValue()
                val newSharesDecimal = if (entity.type == TradeType.BUY) {
                    currentSharesDecimal.subtract(deltaSharesDecimal) // 回滚买入
                } else {
                    currentSharesDecimal.add(deltaSharesDecimal) // 回滚卖出
                }
                
                val updated = asset.withSyncedFields().copy(
                    shares = newSharesDecimal.toDouble(),
                    sharesDecimal = newSharesDecimal
                )
                assetDao.updateAsset(updated)
            }

            // 还原现金 - 使用BigDecimal精确计算
            val cashDeltaDecimal = entity.getAmountValue()
            val finalCashDelta = if (entity.type == TradeType.BUY) {
                cashDeltaDecimal // 回滚买入，退回现金
            } else {
                cashDeltaDecimal.negate() // 回滚卖出，扣除现金
            }
            
            val portfolio = portfolioDao.getPortfolioSuspend()
            if (portfolio != null) {
                val currentCashDecimal = portfolio.getCashValue()
                val newCashDecimal = currentCashDecimal.add(finalCashDelta)
                portfolioDao.updateCash(newCashDecimal.toDouble())
            }

            // 删除交易
            transactionDao.deleteById(entity.id)
        }
    }

    /**
     * 步骤6: 切换到BigDecimal精确计算
     */
    suspend fun updateTransaction(tx: Transaction) {
        db.withTransaction {
            val old = transactionDao.getTransactionById(tx.id.toString())
            if (old != null) {
                // 先回滚旧交易 - 使用BigDecimal精确计算
                old.assetId?.let { aid ->
                    val asset = assetDao.getAssetById(aid) ?: return@withTransaction
                    val currentSharesDecimal = asset.getSharesValue() ?: BigDecimal.ZERO
                    val deltaSharesDecimal = old.getSharesValue()
                    val newSharesDecimal = if (old.type == TradeType.BUY) {
                        currentSharesDecimal.subtract(deltaSharesDecimal) // 回滚买入
                    } else {
                        currentSharesDecimal.add(deltaSharesDecimal) // 回滚卖出
                    }
                    
                    val updated = asset.withSyncedFields().copy(
                        shares = newSharesDecimal.toDouble(),
                        sharesDecimal = newSharesDecimal
                    )
                    assetDao.updateAsset(updated)
                }

                // 回滚现金变动
                val cashDeltaRollbackDecimal = old.getAmountValue()
                val finalCashRollback = if (old.type == TradeType.BUY) {
                    cashDeltaRollbackDecimal // 回滚买入，退回现金
                } else {
                    cashDeltaRollbackDecimal.negate() // 回滚卖出，扣除现金
                }
                
                portfolioDao.getPortfolioSuspend()?.let { portfolio ->
                    val currentCashDecimal = portfolio.getCashValue()
                    val newCashDecimal = currentCashDecimal.add(finalCashRollback)
                    portfolioDao.updateCash(newCashDecimal.toDouble())
                }
            }

            // 应用新交易
            transactionDao.updateTransaction(tx.toEntity())

            // 应用新交易的资产变动 - 使用BigDecimal精确计算
            tx.assetId?.let { aid ->
                val asset = assetDao.getAssetById(aid.toString()) ?: return@withTransaction
                val currentSharesDecimal = asset.getSharesValue() ?: BigDecimal.ZERO
                val deltaSharesDecimal = tx.getSharesValue()
                val newSharesDecimal = if (tx.type == TradeType.BUY) {
                    currentSharesDecimal.add(deltaSharesDecimal)
                } else {
                    currentSharesDecimal.subtract(deltaSharesDecimal)
                }
                
                val updated = asset.withSyncedFields().copy(
                    shares = newSharesDecimal.toDouble(),
                    sharesDecimal = newSharesDecimal
                )
                assetDao.updateAsset(updated)
            }

            // 应用新交易的现金变动
            val cashDeltaApplyDecimal = tx.getAmountValue()
            val finalCashApply = if (tx.type == TradeType.BUY) {
                cashDeltaApplyDecimal.negate() // 买入扣除现金
            } else {
                cashDeltaApplyDecimal // 卖出增加现金
            }
            
            portfolioDao.getPortfolioSuspend()?.let { portfolio ->
                val currentCashDecimal = portfolio.getCashValue()
                val newCashDecimal = currentCashDecimal.add(finalCashApply)
                portfolioDao.updateCash(newCashDecimal.toDouble())
            }
        }
    }

    /**
     * 清空所有交易记录，但不影响资产份额和现金
     * 与deleteTransaction()不同，这个方法只删除交易记录本身
     */
    suspend fun clearAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    /* ---------------------------- 交易机会 CRUD ---------------------------- */
    suspend fun insertTradingOpportunities(items: List<com.example.strategicassetallocationassistant.TradingOpportunity>) {
        tradingOpportunityDao.insertAll(items.map { it.toEntity() })
    }

    suspend fun clearTradingOpportunities() {
        tradingOpportunityDao.deleteAll()
    }

    suspend fun deleteTradingOpportunity(id: UUID) {
        tradingOpportunityDao.deleteById(id.toString())
    }

    /* -------------------- 买入机会顺延标记 -------------------- */

    suspend fun isBuyOpportunityPostponed(): Boolean =
        portfolioDao.getBuyOpportunityPostponed() ?: false

    suspend fun setBuyOpportunityPostponed(flag: Boolean) {
        portfolioDao.updateBuyOpportunityPostponed(flag)
    }

    /* ---------------------------- 买入机会检查时间 ---------------------------- */

    suspend fun getLastBuyOpportunityCheck(): java.time.LocalDateTime? =
        portfolioDao.getLastBuyOpportunityCheck()

    suspend fun updateLastBuyOpportunityCheck(time: java.time.LocalDateTime?) {
        portfolioDao.updateLastBuyOpportunityCheck(time)
    }

    /* ---------------------------- 私有扩展 ---------------------------- */

    private fun AssetEntity.toDomain(): Asset = Asset(
        id = UUID.fromString(id),
        name = name,
        targetWeight = targetWeight,
        code = code,
        shares = shares,
        unitValue = unitValue,
        // 步骤3: 添加BigDecimal字段支持
        sharesDecimal = getSharesValue(),
        unitValueDecimal = getUnitValueValue(),
        lastUpdateTime = lastUpdateTime,
        note = note
    )

    private fun Asset.toEntity(): AssetEntity {
        // 步骤3: 优先使用BigDecimal字段
        val shares = getSharesValue()
        val unitValue = getUnitValueValue()
        
        return AssetEntity.createWithDecimal(
            id = id,
            name = name,
            targetWeight = targetWeight,
            code = code,
            sharesDecimal = shares,
            unitValueDecimal = unitValue,
            lastUpdateTime = lastUpdateTime,
            note = note
        )
    }

    // no longer used combineCashFlow

    /* ---------------------------- 转换 ---------------------------- */
    private fun TransactionEntity.toDomain(): Transaction = Transaction(
        id = java.util.UUID.fromString(id),
        assetId = assetId?.let { java.util.UUID.fromString(it) },
        type = type,
        shares = shares,
        price = price,
        fee = fee,
        amount = amount,
        // 步骤3: 添加BigDecimal字段支持
        sharesDecimal = getSharesValue(),
        priceDecimal = getPriceValue(),
        feeDecimal = getFeeValue(),
        amountDecimal = getAmountValue(),
        time = time,
        reason = reason
    )

    private fun Transaction.toEntity(): TransactionEntity {
        // 步骤3: 优先使用BigDecimal字段
        val shares = getSharesValue()
        val price = getPriceValue()
        val fee = getFeeValue()
        val amount = getAmountValue()
        
        return TransactionEntity.createWithDecimal(
            id = id,
            assetId = assetId,
            type = type,
            sharesDecimal = shares,
            priceDecimal = price,
            feeDecimal = fee,
            amountDecimal = amount,
            time = time,
            reason = reason
        )
    }

    /* ---------------------------- AssetAnalysis 转换 ---------------------------- */
    private fun com.example.strategicassetallocationassistant.data.database.entities.AssetAnalysisEntity.toDomain(): com.example.strategicassetallocationassistant.AssetAnalysis =
        com.example.strategicassetallocationassistant.AssetAnalysis(
            assetId = UUID.fromString(assetId),
            volatility = volatility,
            sevenDayReturn = sevenDayReturn,
            buyFactor = buyFactor,
            sellThreshold = sellThreshold,
            buyFactorLog = buyFactorLog,
            sellThresholdLog = sellThresholdLog,
            relativeOffset = relativeOffset,
            offsetFactor = offsetFactor,
            drawdownFactor = drawdownFactor,
            preVolatilityBuyFactor = preVolatilityBuyFactor,
            assetRisk = assetRisk,
            lastUpdateTime = lastUpdateTime
        )

    private fun com.example.strategicassetallocationassistant.AssetAnalysis.toEntity(): com.example.strategicassetallocationassistant.data.database.entities.AssetAnalysisEntity =
        com.example.strategicassetallocationassistant.data.database.entities.AssetAnalysisEntity.create(
            assetId = assetId.toString(),
            volatility = volatility,
            sevenDayReturn = sevenDayReturn,
            buyFactor = buyFactor,
            sellThreshold = sellThreshold,
            buyFactorLog = buyFactorLog,
            sellThresholdLog = sellThresholdLog,
            relativeOffset = relativeOffset,
            offsetFactor = offsetFactor,
            drawdownFactor = drawdownFactor,
            preVolatilityBuyFactor = preVolatilityBuyFactor,
            assetRisk = assetRisk,
            lastUpdateTime = lastUpdateTime
        )

    private fun TradingOpportunityEntity.toDomain(): com.example.strategicassetallocationassistant.TradingOpportunity =
        com.example.strategicassetallocationassistant.TradingOpportunity(
            id = UUID.fromString(id),
            assetId = assetId?.let { UUID.fromString(it) },
            type = type,
            shares = shares,
            price = price,
            fee = fee,
            amount = amount,
            // 步骤3: 添加BigDecimal字段支持
            sharesDecimal = getSharesValue(),
            priceDecimal = getPriceValue(),
            feeDecimal = getFeeValue(),
            amountDecimal = getAmountValue(),
            time = time,
            reason = reason
        )

    private fun com.example.strategicassetallocationassistant.TradingOpportunity.toEntity(): TradingOpportunityEntity {
        // 步骤3: 优先使用BigDecimal字段
        val shares = getSharesValue()
        val price = getPriceValue()
        val fee = getFeeValue()
        val amount = getAmountValue()
        
        return TradingOpportunityEntity.createWithDecimal(
            id = id,
            assetId = assetId,
            type = type,
            sharesDecimal = shares,
            priceDecimal = price,
            feeDecimal = fee,
            amountDecimal = amount,
            time = time,
            reason = reason
        )
    }
}


