package com.perg.converter.data.converter

import android.content.Context
import android.net.Uri
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument

class DocxToTxtConverter @Inject constructor() {
    suspend fun convert(context: Context, sourceUri: String, outputFile: File) {
        withContext(Dispatchers.IO) {
            val lines = context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                XWPFDocument(input).use { doc ->
                    val out = mutableListOf<String>()
                    for (p in doc.paragraphs) out.add(p.text)
                    for (t in doc.tables) {
                        for (row in t.rows) out.add(row.tableCells.joinToString("  ") { it.text })
                    }
                    out
                }
            }
            outputFile.writeText(lines.joinToString("\n"))
        }
    }
}
