package com.perg.converter.presentation.batch

import com.perg.converter.domain.model.DocumentFormat

enum class BatchStatus {
    QUEUED,
    RUNNING,
    DONE,
    FAILED
}

data class BatchItem(
    val uri: String,
    val name: String,
    val sourceFormat: DocumentFormat,
    val status: BatchStatus = BatchStatus.QUEUED,
    val progress: Int = 0,
    val outputUri: String? = null,
    val error: String? = null
)

data class BatchState(
    val items: List<BatchItem> = emptyList(),
    val targetFormat: DocumentFormat? = null,
    val availableTargets: List<DocumentFormat> = emptyList(),
    val isConverting: Boolean = false,
    val started: Boolean = false,
    val showSummary: Boolean = false
) {
    val doneCount: Int = items.count { it.status == BatchStatus.DONE }
    val finished: Boolean = started && !isConverting && items.isNotEmpty() &&
        items.all { it.status == BatchStatus.DONE || it.status == BatchStatus.FAILED }
}
