package com.perg.converter.data.converter

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TextToPdfConverter @Inject constructor() {
    suspend fun convert(context: Context, sourceUri: String, outputFile: File) {
        withContext(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                input?.bufferedReader()?.readText() ?: throw IllegalArgumentException("ReadFailed")
            }
            val document = PdfDocument()
            try {
                val pageWidth = 595
                val pageHeight = 842
                val margin = 40f
                val paint = Paint().apply { textSize = 12f }
                val lineHeight = paint.fontSpacing
                var pageNumber = 1
                var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).build())
                var y = margin + lineHeight
                val maxWidth = pageWidth - margin * 2
                val lines = mutableListOf<String>()
                for (raw in text.split("\n")) {
                    var remaining = raw
                    while (paint.measureText(remaining) > maxWidth) {
                        var cut = remaining.length
                        while (cut > 0 && paint.measureText(remaining.substring(0, cut)) > maxWidth) cut--
                        lines.add(remaining.substring(0, cut))
                        remaining = remaining.substring(cut)
                    }
                    lines.add(remaining)
                }
                for (line in lines) {
                    if (y + lineHeight > pageHeight - margin) {
                        document.finishPage(page)
                        pageNumber++
                        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).build())
                        y = margin + lineHeight
                    }
                    page.canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                }
                document.finishPage(page)
                outputFile.outputStream().use { out -> document.writeTo(out) }
            } finally {
                document.close()
            }
        }
    }
}
