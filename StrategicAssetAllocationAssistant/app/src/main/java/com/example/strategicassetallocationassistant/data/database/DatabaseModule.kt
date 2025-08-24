package com.example.strategicassetallocationassistant.data.database

import android.content.Context
import com.example.strategicassetallocationassistant.data.database.dao.AssetDao
import com.example.strategicassetallocationassistant.data.database.dao.PortfolioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideAssetDao(appDatabase: AppDatabase): AssetDao {
        return appDatabase.assetDao()
    }

    @Provides
    fun providePortfolioDao(appDatabase: AppDatabase): PortfolioDao {
        return appDatabase.portfolioDao()
    }

    @Provides
    @Singleton
    fun providePortfolioRepository(
        assetDao: AssetDao,
        portfolioDao: PortfolioDao
    ): com.example.strategicassetallocationassistant.data.repository.PortfolioRepository {
        return com.example.strategicassetallocationassistant.data.repository.PortfolioRepository(
            assetDao,
            portfolioDao
        )
    }
}
