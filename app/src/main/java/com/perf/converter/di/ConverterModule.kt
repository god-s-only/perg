package com.perf.converter.di

import android.content.Context
import com.perf.converter.data.converter.ImageToPdfConverter
import com.perf.converter.data.converter.PdfToImageConverter
import com.perf.converter.data.converter.TextToPdfConverter
import com.perf.converter.data.repository.ConverterRepositoryImpl
import com.perf.converter.domain.repository.ConverterRepository
import com.perf.converter.domain.usecase.ConvertFileUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConverterModule {
    @Provides
    @Singleton
    fun provideImageToPdfConverter(): ImageToPdfConverter = ImageToPdfConverter()

    @Provides
    @Singleton
    fun provideTextToPdfConverter(): TextToPdfConverter = TextToPdfConverter()

    @Provides
    @Singleton
    fun providePdfToImageConverter(): PdfToImageConverter = PdfToImageConverter()

    @Provides
    @Singleton
    fun provideConverterRepository(
        @ApplicationContext context: Context,
        imageToPdf: ImageToPdfConverter,
        textToPdf: TextToPdfConverter,
        pdfToImage: PdfToImageConverter
    ): ConverterRepository = ConverterRepositoryImpl(context, imageToPdf, textToPdf, pdfToImage)

    @Provides
    @Singleton
    fun provideConvertFileUseCase(repository: ConverterRepository): ConvertFileUseCase = ConvertFileUseCase(repository)
}
