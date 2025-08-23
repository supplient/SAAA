package com.example.strategicassetallocationassistant.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.strategicassetallocationassistant.data.database.converters.Converters
import com.example.strategicassetallocationassistant.data.database.entities.*
import com.example.strategicassetallocationassistant.data.database.dao.*

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
    }
}
