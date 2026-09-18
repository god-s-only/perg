package com.perg.converter.presentation.converter

import com.perg.converter.domain.model.DocumentFormat

sealed interface ConverterEvent {
    data class SourcePicked(val uri: String, val fileName: String) : ConverterEvent
    data class TargetSelected(val format: DocumentFormat) : ConverterEvent
    data object StartConversion : ConverterEvent
    data class ConfirmRename(val name: String) : ConverterEvent
    data object DismissError : ConverterEvent
    data object Reset : ConverterEvent
}
