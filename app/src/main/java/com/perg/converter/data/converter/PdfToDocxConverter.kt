package com.perg.converter.data.converter

import android.content.Context
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument

class PdfToDocxConverter @Inject constructor(private val extractor: PdfTextExtractor) {
    suspend fun convert(context: Context, sourceUri: String, outputFile: File) {
        withContext(Dispatchers.IO) {
            val text = extractor.extract(context, sourceUri)
            XWPFDocument().use { doc ->
                for (line in text.split("\n")) {
                    doc.createParagraph().createRun().setText(line)
                }
                outputFile.outputStream().use { out -> doc.write(out) }
            }
        }
    }
}
