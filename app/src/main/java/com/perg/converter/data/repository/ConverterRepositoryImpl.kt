package com.perg.converter.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.perg.converter.data.converter.ImageToPdfConverter
import com.perg.converter.data.converter.PdfToImageConverter
import com.perg.converter.data.converter.TextToPdfConverter
import com.perg.converter.domain.model.ConversionJob
import com.perg.converter.domain.model.ConversionStatus
import com.perg.converter.domain.model.DocumentFormat
import com.perg.converter.domain.repository.ConverterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
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
        val tmp = File(context.cacheDir, job.id + ".pdf")
        imageToPdf.convert(context, job.sourceUri, tmp)
        return publish(tmp, baseName(job) + ".pdf", mimeOf(DocumentFormat.PDF))
    }

    private suspend fun textPdf(job: ConversionJob): String {
        val tmp = File(context.cacheDir, job.id + ".pdf")
        textToPdf.convert(context, job.sourceUri, tmp)
        return publish(tmp, baseName(job) + ".pdf", mimeOf(DocumentFormat.PDF))
    }

    private suspend fun pdfImages(job: ConversionJob): String {
        val tmpDir = File(context.cacheDir, job.id)
        tmpDir.mkdirs()
        val files = pdfToImage.convert(context, job.sourceUri, tmpDir, job.targetFormat)
        val base = baseName(job)
        val ext = if (job.targetFormat == DocumentFormat.JPEG) "jpg" else "png"
        var first = ""
        files.forEachIndexed { index, file ->
            val uri = publish(file, base + "_page" + (index + 1) + "." + ext, mimeOf(job.targetFormat))
            if (index == 0) first = uri
        }
        tmpDir.deleteRecursively()
        return first
    }

    private fun publish(tmp: File, displayName: String, mime: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/Perg")
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val dest = context.contentResolver.insert(collection, values) ?: throw IllegalStateException("SaveFailed")
            context.contentResolver.openOutputStream(dest)?.use { out ->
                tmp.inputStream().use { it.copyTo(out) }
            } ?: throw IllegalStateException("SaveFailed")
            tmp.delete()
            return dest.toString()
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Perg")
            dir.mkdirs()
            val dest = File(dir, displayName)
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
            return Uri.fromFile(dest).toString()
        }
    }

    private fun baseName(job: ConversionJob): String {
        val full = queryDisplayName(job.sourceUri) ?: ("file_" + job.id.take(8))
        return full.substringBeforeLast('.').ifEmpty { "file_" + job.id.take(8) }
    }

    private fun queryDisplayName(uri: String): String? {
        return try {
            context.contentResolver.query(Uri.parse(uri), null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mimeOf(format: DocumentFormat): String {
        return when (format) {
            DocumentFormat.PDF -> "application/pdf"
            DocumentFormat.PNG -> "image/png"
            DocumentFormat.JPEG -> "image/jpeg"
            DocumentFormat.WEBP -> "image/webp"
            DocumentFormat.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            DocumentFormat.TXT -> "text/plain"
        }
    }
}
