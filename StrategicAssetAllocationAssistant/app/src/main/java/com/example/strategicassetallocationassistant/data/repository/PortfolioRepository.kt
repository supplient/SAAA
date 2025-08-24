package com.example.strategicassetallocationassistant.data.repository

import com.example.strategicassetallocationassistant.Asset
import com.example.strategicassetallocationassistant.Portfolio
import com.example.strategicassetallocationassistant.data.database.dao.AssetDao
import com.example.strategicassetallocationassistant.data.database.dao.PortfolioDao
import com.example.strategicassetallocationassistant.data.database.entities.AssetEntity
import com.example.strategicassetallocationassistant.data.database.entities.PortfolioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for interacting with Room database and converting
 * entities into domain models that the rest of the application can use.
 *
 * NOTE: 生产环境下应当通过依赖注入 (Hilt) 获取 DAO 对象；此处直接在构造函数中传入，
 * 以便在不使用 DI 的情况下保持简单。
 */
@Singleton
class PortfolioRepository @Inject constructor(
    private val assetDao: AssetDao,
    private val portfolioDao: PortfolioDao
) {

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
    val portfolioFlow: Flow<Portfolio> = assetsFlow.combineCashFlow()

    /**
     * Suspended version that returns the latest [Portfolio] once.
     */
    suspend fun getPortfolioOnce(): Portfolio {
        val assets = assetDao.getAllAssets().first()
        val cash = portfolioDao.getCash() ?: 0.0
        return Portfolio(assets.map { it.toDomain() }, cash)
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

    /* ---------------------------- 私有扩展 ---------------------------- */

    private fun AssetEntity.toDomain(): Asset = Asset(
        id = UUID.fromString(id),
        name = name,
        type = type,
        targetWeight = targetWeight,
        code = code,
        shares = shares,
        unitValue = unitValue,
        lastUpdateTime = lastUpdateTime
    )

    private fun Asset.toEntity(): AssetEntity = AssetEntity.create(
        id = id,
        name = name,
        type = type,
        targetWeight = targetWeight,
        code = code,
        shares = shares,
        unitValue = unitValue,
        lastUpdateTime = lastUpdateTime
    )

    /**
     * Combine assetsFlow with cash Flow to produce Portfolio objects
     */
    private fun Flow<List<Asset>>.combineCashFlow(): Flow<Portfolio> {
        val cashFlow = portfolioDao.getCashFlow()
        return kotlinx.coroutines.flow.combine(this, cashFlow) { assets, cash ->
            Portfolio(assets, cash ?: 0.0)
        }
    }
}


