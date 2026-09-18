package com.perg.converter.data.repository

import android.content.Context
import android.net.Uri
import com.perg.converter.data.converter.ImageToPdfConverter
import com.perg.converter.data.converter.PdfToImageConverter
import com.perg.converter.data.converter.TextToPdfConverter
import com.perg.converter.domain.model.ConversionJob
import com.perg.converter.domain.model.ConversionStatus
import com.perg.converter.domain.model.DocumentFormat
import com.perg.converter.domain.repository.ConverterRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Singleton
class ConverterRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageToPdf: ImageToPdfConverter,
    private val textToPdf: TextToPdfConverter,
    private val pdfToImage: PdfToImageConverter
) : ConverterRepository {
    override fun convert(job: ConversionJob): Flow<ConversionJob> = flow {
        emit(job.copy(status = ConversionStatus.QUEUED, progress = 0))
        emit(job.copy(status = ConversionStatus.RUNNING, progress = 10))
        try {
            val output = when {
                job.sourceFormat == DocumentFormat.JPEG && job.targetFormat == DocumentFormat.PDF -> singlePdf(job)
                job.sourceFormat == DocumentFormat.PNG && job.targetFormat == DocumentFormat.PDF -> singlePdf(job)
                job.sourceFormat == DocumentFormat.WEBP && job.targetFormat == DocumentFormat.PDF -> singlePdf(job)
                job.sourceFormat == DocumentFormat.TXT && job.targetFormat == DocumentFormat.PDF -> textPdf(job)
                job.sourceFormat == DocumentFormat.PDF && job.targetFormat == DocumentFormat.PNG -> pdfImages(job)
                job.sourceFormat == DocumentFormat.PDF && job.targetFormat == DocumentFormat.JPEG -> pdfImages(job)
                else -> throw IllegalArgumentException("UnsupportedConversion")
            }
            emit(job.copy(status = ConversionStatus.SUCCEEDED, progress = 100, outputUri = output))
        } catch (e: Exception) {
            emit(job.copy(status = ConversionStatus.FAILED, error = e.message))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun singlePdf(job: ConversionJob): String {
        val outFile = File(context.cacheDir, job.id + ".pdf")
        imageToPdf.convert(context, job.sourceUri, outFile)
        return Uri.fromFile(outFile).toString()
    }

    private suspend fun textPdf(job: ConversionJob): String {
        val outFile = File(context.cacheDir, job.id + ".pdf")
        textToPdf.convert(context, job.sourceUri, outFile)
        return Uri.fromFile(outFile).toString()
    }

    private suspend fun pdfImages(job: ConversionJob): String {
        val outDir = File(context.cacheDir, job.id)
        outDir.mkdirs()
        pdfToImage.convert(context, job.sourceUri, outDir, job.targetFormat)
        return Uri.fromFile(outDir).toString()
    }
}
