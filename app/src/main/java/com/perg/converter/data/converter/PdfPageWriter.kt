package com.perg.converter.data.converter

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import javax.inject.Inject

class PdfPageWriter @Inject constructor() {
    fun writeLines(lines: List<String>, outputFile: File) {
        val document = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val paint = Paint().apply { textSize = 12f }
            val lineHeight = paint.fontSpacing
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var y = margin + lineHeight
            val maxWidth = pageWidth - margin * 2
            val wrapped = mutableListOf<String>()
            for (raw in lines) {
                var remaining = raw
                if (remaining.isEmpty()) {
                    wrapped.add("")
                    continue
                }
                while (paint.measureText(remaining) > maxWidth) {
                    var cut = remaining.length
                    while (cut > 0 && paint.measureText(remaining.substring(0, cut)) > maxWidth) cut--
                    wrapped.add(remaining.substring(0, cut))
                    remaining = remaining.substring(cut)
                }
                wrapped.add(remaining)
            }
            for (line in wrapped) {
                if (y + lineHeight > pageHeight - margin) {
                    document.finishPage(page)
                    pageNumber++
                    page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
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
