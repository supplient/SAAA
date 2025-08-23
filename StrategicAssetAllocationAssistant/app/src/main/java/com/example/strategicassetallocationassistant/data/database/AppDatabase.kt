package com.example.strategicassetallocationassistant.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.strategicassetallocationassistant.data.database.converters.Converters
import com.example.strategicassetallocationassistant.data.database.entities.*
import com.example.strategicassetallocationassistant.data.database.dao.*
import com.example.strategicassetallocationassistant.AssetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 战略资产配置助手应用数据库
 * 
 * 包含以下表：
 * - assets: 资产主表
 * - portfolio: 投资组合表（现金信息）
 */
@Database(
    entities = [
        AssetEntity::class,
        PortfolioEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // DAO 接口
    abstract fun assetDao(): AssetDao
    abstract fun portfolioDao(): PortfolioDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例（单例模式）
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "strategic_asset_allocation_database"
                )
                    .fallbackToDestructiveMigration() // 开发阶段使用，生产环境需要实现Migration
                    .addCallback(PrepopulateCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 清除数据库实例（主要用于测试）
         */
        fun clearInstance() {
            INSTANCE = null
        }

        /**
         * 数据库创建时预填充示例数据
         */
        private class PrepopulateCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 使用IO调度器在后台线程插入数据
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getDatabase(context)
                    val assetDao = database.assetDao()
                    val portfolioDao = database.portfolioDao()

                    val now = LocalDateTime.now()

                    val sampleAssets = listOf(
                        AssetEntity.create(
                            id = UUID.randomUUID(),
                            name = "腾讯控股",
                            type = AssetType.STOCK,
                            targetWeight = 0.15,
                            code = "00700",
                            shares = 200.0,
                            unitValue = 380.0,
                            lastUpdateTime = now.minusHours(1)
                        ),
                        AssetEntity.create(
                            id = UUID.randomUUID(),
                            name = "阿里巴巴",
                            type = AssetType.STOCK,
                            targetWeight = 0.15,
                            code = "09988",
                            shares = 300.0,
                            unitValue = 85.0,
                            lastUpdateTime = now.minusHours(1)
                        ),
                        AssetEntity.create(
                            id = UUID.randomUUID(),
                            name = "易方达蓝筹精选混合",
                            type = AssetType.OFFSHORE_FUND,
                            targetWeight = 0.30,
                            code = "005827",
                            shares = 1000.0,
                            unitValue = 2.15,
                            lastUpdateTime = now.minusDays(1)
                        ),
                        AssetEntity.create(
                            id = UUID.randomUUID(),
                            name = "余额宝货币基金",
                            type = AssetType.MONEY_FUND,
                            targetWeight = 0.10,
                            code = "000198",
                            shares = 50000.0,
                            unitValue = 1.0,
                            lastUpdateTime = now.minusDays(1)
                        )
                    )

                    assetDao.insertAssets(sampleAssets)

                    // 初始化现金记录
                    portfolioDao.insertPortfolio(PortfolioEntity(cash = 10000.0))
                }
            }
        }
    }
}
