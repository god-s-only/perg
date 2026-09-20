package com.perg.converter.presentation.batch

import com.perg.converter.domain.model.DocumentFormat

data class PickedFile(
    val uri: String,
    val name: String,
    val format: DocumentFormat
)

sealed interface BatchEvent {
    data class FilesPicked(val items: List<PickedFile>) : BatchEvent
    data class RemoveFile(val uri: String) : BatchEvent
    data class TargetSelected(val format: DocumentFormat) : BatchEvent
    data object StartBatch : BatchEvent
    data object Reset : BatchEvent
}
