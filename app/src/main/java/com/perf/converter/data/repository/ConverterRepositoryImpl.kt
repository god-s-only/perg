package com.perf.converter.data.repository

import android.content.Context
import android.net.Uri
import com.perf.converter.data.converter.ImageToPdfConverter
import com.perf.converter.data.converter.PdfToImageConverter
import com.perf.converter.data.converter.TextToPdfConverter
import com.perf.converter.domain.model.ConversionJob
import com.perf.converter.domain.model.ConversionStatus
import com.perf.converter.domain.model.DocumentFormat
import com.perf.converter.domain.repository.ConverterRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ConverterRepositoryImpl(
    private val context: Context,
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
