package com.perg.converter.data.converter

import android.content.Context
import android.net.Uri
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument

class TxtToDocxConverter @Inject constructor() {
    suspend fun convert(context: Context, sourceUri: String, outputFile: File) {
        withContext(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                input?.bufferedReader()?.readText() ?: throw IllegalArgumentException("ReadFailed")
            }
            XWPFDocument().use { doc ->
                for (line in text.split("\n")) {
                    doc.createParagraph().createRun().setText(line)
                }
                outputFile.outputStream().use { out -> doc.write(out) }
            }
        }
    }
}
