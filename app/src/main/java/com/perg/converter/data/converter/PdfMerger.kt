package com.perg.converter.data.converter

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfMerger @Inject constructor() {
    suspend fun merge(
        context: Context,
        sourceUris: List<String>,
        outputFile: File,
        onOpened: suspend (Int) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            val merger = PDFMergerUtility()
            val streams = mutableListOf<InputStream>()
            try {
                sourceUris.forEachIndexed { index, uri ->
                    val stream = context.contentResolver.openInputStream(Uri.parse(uri))
                        ?: throw IllegalArgumentException("ReadFailed")
                    streams.add(stream)
                    merger.addSource(stream)
                    onOpened(index + 1)
                }
                outputFile.outputStream().use { out ->
                    merger.destinationStream = out
                    merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
                }
            } finally {
                streams.forEach { runCatching { it.close() } }
            }
        }
    }
}
