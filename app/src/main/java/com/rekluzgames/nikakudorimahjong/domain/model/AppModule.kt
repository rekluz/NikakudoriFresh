package com.rekluzgames.nikakudorimahjong.domain.model

import android.content.Context
import com.rekluzgames.nikakudorimahjong.data.preference.PreferenceManager
import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.engine.GameEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager =
        PreferenceManager(context)

    @Provides
    @Singleton
    fun provideGameRepository(prefs: PreferenceManager): GameRepository =
        GameRepository(prefs)

    @Provides
    @Singleton
    fun provideGameEngine(): GameEngine = GameEngine()

    @Provides
    @Singleton
    fun provideQuoteManager(): QuoteManager = QuoteManager()
}