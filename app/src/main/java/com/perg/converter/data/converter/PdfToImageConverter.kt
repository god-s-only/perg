package com.perg.converter.data.converter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.perg.converter.domain.model.DocumentFormat
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfToImageConverter @Inject constructor() {
    suspend fun convert(
        context: Context,
        sourceUri: String,
        outputDir: File,
        target: DocumentFormat
    ): List<File> {
        return withContext(Dispatchers.IO) {
            val fd: ParcelFileDescriptor =
                context.contentResolver.openFileDescriptor(Uri.parse(sourceUri), "r")
                    ?: throw IllegalArgumentException("OpenFailed")
            fd.use {
                val renderer = PdfRenderer(it)
                try {
                    val files = mutableListOf<File>()
                    val compress = if (target == DocumentFormat.JPEG) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
                    val ext = if (target == DocumentFormat.JPEG) "jpg" else "png"
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, Bitmap.Config.ARGB_8888)
                            val outFile = File(outputDir, "page_${i + 1}.$ext")
                            outFile.outputStream().use { out -> bitmap.compress(compress, 95, out) }
                            bitmap.recycle()
                            files.add(outFile)
                        }
                    }
                    files
                } finally {
                    renderer.close()
                }
            }
        }
    }
}
