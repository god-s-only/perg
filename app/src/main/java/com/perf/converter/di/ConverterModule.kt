package com.perf.converter.di

import com.perf.converter.data.repository.ConverterRepositoryImpl
import com.perf.converter.domain.repository.ConverterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConverterModule {
    @Binds
    @Singleton
    abstract fun bindConverterRepository(impl: ConverterRepositoryImpl): ConverterRepository
}
