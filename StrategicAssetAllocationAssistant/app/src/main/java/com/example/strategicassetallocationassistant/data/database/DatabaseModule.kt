package com.example.strategicassetallocationassistant.data.database

import android.content.Context
import com.example.strategicassetallocationassistant.data.database.dao.AssetDao
import com.example.strategicassetallocationassistant.data.database.dao.PortfolioDao
import com.example.strategicassetallocationassistant.data.database.dao.TransactionDao
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
    fun provideTransactionDao(appDatabase: AppDatabase): TransactionDao {
        return appDatabase.transactionDao()
    }

    @Provides
    @Singleton
    fun providePortfolioRepository(
        appDatabase: AppDatabase,
        assetDao: AssetDao,
        portfolioDao: PortfolioDao,
        transactionDao: TransactionDao
    ): com.example.strategicassetallocationassistant.data.repository.PortfolioRepository {
        return com.example.strategicassetallocationassistant.data.repository.PortfolioRepository(
            assetDao,
            portfolioDao,
            transactionDao,
            appDatabase
        )
    }
}
