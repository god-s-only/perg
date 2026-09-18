package com.perf.converter.domain.model

/**
 * Formats supported for 100% offline conversion.
 *
 * Decoding/encoding is done on-device only:
 * - Images via Android BitmapFactory / PdfDocument / PdfRenderer
 * - PDF via android.graphics.pdf.PdfDocument (write) + PdfRenderer (read)
 * - DOCX via Apache POI (read/write) rendered to PDF with PdfDocument
 * - TXT via plain I/O, rendered to PDF with PdfDocument
 */
enum class DocumentFormat(val extension: String) {
    JPEG("jpg"),
    PNG("png"),
    WEBP("webp"),
    PDF("pdf"),
    DOCX("docx"),
    TXT("txt");

    companion object {
        fun fromFileName(fileName: String): DocumentFormat? {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extension == ext || (it == JPEG && ext == "jpeg") }
        }
    }
}

enum class ConversionStatus {
    IDLE,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED
}

data class ConversionJob(
    val id: String,
    val sourceUri: String,
    val sourceFormat: DocumentFormat,
    val targetFormat: DocumentFormat,
    val status: ConversionStatus = ConversionStatus.IDLE,
    val progress: Int = 0,
    val outputUri: String? = null,
    val error: String? = null
)

/**
 * Offline capability matrix. Only pairs listed here are offered in UI.
 * v1 focuses on reliable on-device paths; everything else returns
 * UnsupportedConversion.
 */
object ConversionCapabilities {
    private val supported: Set<Pair<DocumentFormat, DocumentFormat>> = setOf(
        // Images -> PDF (PdfDocument, 1 image = 1 page)
        Pair(DocumentFormat.JPEG, DocumentFormat.PDF),
        Pair(DocumentFormat.PNG, DocumentFormat.PDF),
        Pair(DocumentFormat.WEBP, DocumentFormat.PDF),
        // Text / Office -> PDF (POI / plain text -> PdfDocument pages)
        Pair(DocumentFormat.TXT, DocumentFormat.PDF),
        Pair(DocumentFormat.DOCX, DocumentFormat.PDF),
        // PDF -> image/text office (PdfRenderer -> Bitmap, PDFBox text -> POI/TXT)
        Pair(DocumentFormat.PDF, DocumentFormat.PNG),
        Pair(DocumentFormat.PDF, DocumentFormat.JPEG),
        Pair(DocumentFormat.PDF, DocumentFormat.TXT),
        Pair(DocumentFormat.PDF, DocumentFormat.DOCX),
        // Plain text <-> Office (POI, no PDF involved)
        Pair(DocumentFormat.TXT, DocumentFormat.DOCX),
        Pair(DocumentFormat.DOCX, DocumentFormat.TXT)
    )

    fun isSupported(source: DocumentFormat, target: DocumentFormat): Boolean =
        supported.contains(Pair(source, target))

    fun targetsFor(source: DocumentFormat): List<DocumentFormat> =
        supported.filter { it.first == source }.map { it.second }.distinct()
}
