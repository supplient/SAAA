package com.example.strategicassetallocationassistant.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.strategicassetallocationassistant.data.database.converters.Converters
import com.example.strategicassetallocationassistant.data.database.entities.*
import com.example.strategicassetallocationassistant.data.database.dao.*
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
        PortfolioEntity::class,
        com.example.strategicassetallocationassistant.data.database.entities.TransactionEntity::class,
        com.example.strategicassetallocationassistant.data.database.entities.TradingOpportunityEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // DAO 接口
    abstract fun assetDao(): AssetDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun transactionDao(): com.example.strategicassetallocationassistant.data.database.dao.TransactionDao
    abstract fun tradingOpportunityDao(): com.example.strategicassetallocationassistant.data.database.dao.TradingOpportunityDao
    
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
                    .addMigrations(object : androidx.room.migration.Migration(1, 2) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            // 1) 将 MONEY_FUND 合并进 cash
                            // 读取 MONEY_FUND 的总市值（shares*unitValue; unitValue 为空则按1.0）
                            db.execSQL("""
                                CREATE TABLE IF NOT EXISTS assets_tmp_value AS
                                SELECT id, name, targetWeight, code, shares, unitValue, lastUpdateTime, note,
                                       (COALESCE(shares,0) * COALESCE(unitValue,1.0)) AS mv,
                                       (CASE WHEN type='MONEY_FUND' THEN 1 ELSE 0 END) AS is_money
                                FROM assets
                            """.trimIndent())

                            val cursor = db.query("SELECT COALESCE(SUM(mv),0) FROM assets_tmp_value WHERE is_money=1")
                            var addCash = 0.0
                            if (cursor.moveToFirst()) {
                                addCash = cursor.getDouble(0)
                            }
                            cursor.close()

                            // 更新 portfolio.cash
                            val cur = db.query("SELECT cash FROM portfolio WHERE id=1")
                            if (cur.moveToFirst()) {
                                val cash = cur.getDouble(0)
                                db.execSQL("UPDATE portfolio SET cash=? WHERE id=1", arrayOf(cash + addCash))
                            } else {
                                // 若不存在则插入一条
                                db.execSQL("INSERT INTO portfolio(id, cash) VALUES(1, ?)", arrayOf(addCash))
                            }
                            cur.close()

                            // 2) 删除 MONEY_FUND 资产
                            db.execSQL("DELETE FROM assets WHERE type='MONEY_FUND'")

                            // 3) 重建 assets 无 type 列
                            db.execSQL("""
                                CREATE TABLE IF NOT EXISTS assets_new (
                                    id TEXT NOT NULL PRIMARY KEY,
                                    name TEXT NOT NULL,
                                    targetWeight REAL NOT NULL,
                                    code TEXT,
                                    shares REAL,
                                    unitValue REAL,
                                    lastUpdateTime TEXT,
                                    note TEXT
                                )
                            """.trimIndent())
                            db.execSQL("""
                                INSERT INTO assets_new(id, name, targetWeight, code, shares, unitValue, lastUpdateTime, note)
                                SELECT id, name, targetWeight, code, shares, unitValue, lastUpdateTime, note
                                FROM assets
                                WHERE type!='MONEY_FUND'
                            """.trimIndent())
                            db.execSQL("DROP TABLE assets")
                            db.execSQL("ALTER TABLE assets_new RENAME TO assets")
                            db.execSQL("DROP TABLE assets_tmp_value")
                        }
                    })
                    .addMigrations(object : androidx.room.migration.Migration(2, 3) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            // Add volatility column with nullable REAL
                            db.execSQL("ALTER TABLE assets ADD COLUMN volatility REAL")
                        }
                    })
                    .addMigrations(object : androidx.room.migration.Migration(3, 4) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE assets ADD COLUMN sevenDayReturn REAL")
                        }
                    })
                    .addMigrations(object : androidx.room.migration.Migration(4, 5) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE assets ADD COLUMN offsetFactor REAL")
                            db.execSQL("ALTER TABLE assets ADD COLUMN drawdownFactor REAL")
                            db.execSQL("ALTER TABLE assets ADD COLUMN buyFactor REAL")
                        }
                    })
                    .addCallback(PrepopulateCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
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
                            name = "贵州茅台",
                            targetWeight = 0.5,
                            code = "sh600519",
                            shares = 100.0,
                            unitValue = 1.0,
                            lastUpdateTime = now.minusHours(1)
                        ),
                        AssetEntity.create(
                            id = UUID.randomUUID(),
                            name = "标普500ETF",
                            targetWeight = 0.5,
                            code = "sh513500",
                            shares = 100.0,
                            unitValue = 1.0,
                            lastUpdateTime = now.minusHours(1)
                        ),
                    )

                    assetDao.insertAssets(sampleAssets)

                    // 初始化现金记录
                    portfolioDao.insertPortfolio(PortfolioEntity(cash = 10000.0))
                }
            }
        }
    }
}
