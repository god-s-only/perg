package com.perg.converter.data.converter

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageToPdfConverter @Inject constructor() {
    suspend fun convert(context: Context, sourceUri: String, outputFile: File) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: throw IllegalArgumentException("DecodeFailed")
                val document = PdfDocument()
                try {
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).build()
                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                    outputFile.outputStream().use { out -> document.writeTo(out) }
                } finally {
                    document.close()
                }
            }
        }
    }
}
