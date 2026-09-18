package com.perf.converter.domain.model

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

object ConversionCapabilities {
    private val supported: Set<Pair<DocumentFormat, DocumentFormat>> = setOf(
        Pair(DocumentFormat.JPEG, DocumentFormat.PDF),
        Pair(DocumentFormat.PNG, DocumentFormat.PDF),
        Pair(DocumentFormat.WEBP, DocumentFormat.PDF),
        Pair(DocumentFormat.TXT, DocumentFormat.PDF),
        Pair(DocumentFormat.DOCX, DocumentFormat.PDF),
        Pair(DocumentFormat.PDF, DocumentFormat.PNG),
        Pair(DocumentFormat.PDF, DocumentFormat.JPEG),
        Pair(DocumentFormat.PDF, DocumentFormat.TXT),
        Pair(DocumentFormat.PDF, DocumentFormat.DOCX),
        Pair(DocumentFormat.TXT, DocumentFormat.DOCX),
        Pair(DocumentFormat.DOCX, DocumentFormat.TXT)
    )

    fun isSupported(source: DocumentFormat, target: DocumentFormat): Boolean =
        supported.contains(Pair(source, target))

    fun targetsFor(source: DocumentFormat): List<DocumentFormat> =
        supported.filter { it.first == source }.map { it.second }.distinct()
}
