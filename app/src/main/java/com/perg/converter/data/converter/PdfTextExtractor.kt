package com.perg.converter.data.converter

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfTextExtractor @Inject constructor() {
    suspend fun extract(context: Context, sourceUri: String): String {
        return withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            val text = context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                PDDocument.load(input).use { doc ->
                    PDFTextStripper().getText(doc)
                }
            } ?: throw IllegalArgumentException("ReadFailed")
            if (text.isBlank()) throw IllegalArgumentException("EmptyDocument")
            text
        }
    }
}
