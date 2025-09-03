package com.example.strategicassetallocationassistant.data.repository

import androidx.room.withTransaction
import com.example.strategicassetallocationassistant.Asset
import com.example.strategicassetallocationassistant.Portfolio
import com.example.strategicassetallocationassistant.TradeType
import com.example.strategicassetallocationassistant.TradingOpportunity
import com.example.strategicassetallocationassistant.Transaction
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
            assetDao.deleteAllAssets()
            portfolioDao.deletePortfolio()
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
     */
    val portfolioFlow: Flow<Portfolio> = kotlinx.coroutines.flow.combine(
        portfolioDao.getPortfolio(),
        assetsFlow
    ) { entity, assets ->
        Portfolio(
            assets = assets,
            cash = entity?.cash ?: 0.0,
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
        return Portfolio(assets.map { it.toDomain() }, cash, note)
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
        val current = portfolioDao.getPortfolioSuspend()
        if (current == null) {
            // Insert new record if not exist
            portfolioDao.insertPortfolio(PortfolioEntity(cash = cash))
        } else {
            portfolioDao.updateCash(cash)
        }
    }

    suspend fun updateNote(note: String?) {
        val current = portfolioDao.getPortfolioSuspend()
        if (current == null) {
            // insert new record with note
            portfolioDao.insertPortfolio(PortfolioEntity(cash = 0.0, note = note))
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
     * 更新资产的买入因子和计算过程日志
     */
    suspend fun updateAssetBuyFactorWithLog(assetId: UUID, buyFactor: Double, buyFactorLog: String) {
        val existing = assetAnalysisDao.getAssetAnalysisById(assetId.toString())?.toDomain()
            ?: com.example.strategicassetallocationassistant.AssetAnalysis(assetId = assetId)

        val updated = existing.copy(
            buyFactor = buyFactor,
            buyFactorLog = buyFactorLog
        )
        upsertAssetAnalysis(updated)
    }

    /**
     * 更新资产的卖出阈值和计算过程日志
     */
    suspend fun updateAssetSellThresholdWithLog(assetId: UUID, sellThreshold: Double, sellThresholdLog: String) {
        val existing = assetAnalysisDao.getAssetAnalysisById(assetId.toString())?.toDomain()
            ?: com.example.strategicassetallocationassistant.AssetAnalysis(assetId = assetId)

        val updated = existing.copy(
            sellThreshold = sellThreshold,
            sellThresholdLog = sellThresholdLog
        )
        upsertAssetAnalysis(updated)
    }

    /**
     * 添加一条交易记录，同时原子更新资产份额和现金。
     */
    suspend fun addTransaction(tx: Transaction) {
        db.withTransaction {
            // 插入交易
            transactionDao.insertTransaction(tx.toEntity())

            // 更新资产 shares
            tx.assetId?.let { aid ->
                val asset = assetDao.getAssetById(aid.toString()) ?: return@withTransaction
                val currentShares = asset.shares ?: 0.0
                val deltaShares = if (tx.type == TradeType.BUY) tx.shares else -tx.shares
                val updated = asset.copy(shares = currentShares + deltaShares)
                assetDao.updateAsset(updated)
            }

            // 更新现金
            val cashDelta = if (tx.type == TradeType.BUY) -tx.amount else tx.amount
            val currentPortfolio = portfolioDao.getPortfolioSuspend()
            if (currentPortfolio == null) {
                portfolioDao.insertPortfolio(PortfolioEntity(cash = cashDelta))
            } else {
                portfolioDao.updateCash(currentPortfolio.cash + cashDelta)
            }
        }
    }

    suspend fun getTransactionById(id: java.util.UUID): Transaction? {
        return transactionDao.getAllTransactions().first().firstOrNull { java.util.UUID.fromString(it.id) == id }?.toDomain()
    }

    suspend fun deleteTransaction(tx: Transaction) {
        db.withTransaction {
            // 先获取数据库中的最新交易记录，以免调用方传入的是过时对象
            val entity = transactionDao.getTransactionById(tx.id.toString()) ?: return@withTransaction

            // 还原资产 shares
            entity.assetId?.let { aid ->
                val asset = assetDao.getAssetById(aid) ?: return@withTransaction
                val currentShares = asset.shares ?: 0.0
                val deltaShares = if (entity.type == TradeType.BUY) -entity.shares else entity.shares
                assetDao.updateAsset(asset.copy(shares = currentShares + deltaShares))
            }

            // 还原现金
            val cashDelta = if (entity.type == TradeType.BUY) entity.amount else -entity.amount
            val portfolio = portfolioDao.getPortfolioSuspend()
            if (portfolio != null) {
                portfolioDao.updateCash(portfolio.cash + cashDelta)
            }

            // 删除交易
            transactionDao.deleteById(entity.id)
        }
    }

    suspend fun updateTransaction(tx: Transaction) {
        db.withTransaction {
            val old = transactionDao.getTransactionById(tx.id.toString())
            if (old != null) {
                // 先回滚旧交易
                old.assetId?.let { aid ->
                    val asset = assetDao.getAssetById(aid) ?: return@withTransaction
                    val currentShares = asset.shares ?: 0.0
                    val deltaShares = if (old.type == TradeType.BUY) -old.shares else old.shares
                    assetDao.updateAsset(asset.copy(shares = currentShares + deltaShares))
                }

                val cashDeltaRollback = if (old.type == TradeType.BUY) old.amount else -old.amount
                portfolioDao.getPortfolioSuspend()?.let { portfolioDao.updateCash(it.cash + cashDeltaRollback) }
            }

            // 应用新交易
            transactionDao.updateTransaction(tx.toEntity())

            tx.assetId?.let { aid ->
                val asset = assetDao.getAssetById(aid.toString()) ?: return@withTransaction
                val currentShares = asset.shares ?: 0.0
                val deltaShares = if (tx.type == TradeType.BUY) tx.shares else -tx.shares
                assetDao.updateAsset(asset.copy(shares = currentShares + deltaShares))
            }

            val cashDeltaApply = if (tx.type == TradeType.BUY) -tx.amount else tx.amount
            portfolioDao.getPortfolioSuspend()?.let { portfolioDao.updateCash(it.cash + cashDeltaApply) }
        }
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
        lastUpdateTime = lastUpdateTime,
        note = note
    )

    private fun Asset.toEntity(): AssetEntity = AssetEntity.create(
        id = id,
        name = name,
        targetWeight = targetWeight,
        code = code,
        shares = shares,
        unitValue = unitValue,
        lastUpdateTime = lastUpdateTime,
        note = note
    )

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
        time = time,
        reason = reason
    )

    private fun Transaction.toEntity(): TransactionEntity = TransactionEntity.create(
        id = id,
        assetId = assetId,
        type = type,
        shares = shares,
        price = price,
        fee = fee,
        amount = amount,
        time = time,
        reason = reason
    )

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
            time = time,
            reason = reason
        )

    private fun com.example.strategicassetallocationassistant.TradingOpportunity.toEntity(): TradingOpportunityEntity =
        TradingOpportunityEntity.create(
            id = id,
            assetId = assetId,
            type = type,
            shares = shares,
            price = price,
            fee = fee,
            amount = amount,
            time = time,
            reason = reason
        )
}


