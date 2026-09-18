package com.perf.converter.presentation.converter

import com.perf.converter.domain.model.DocumentFormat

sealed interface ConverterEvent {
    data class SourcePicked(val uri: String, val fileName: String) : ConverterEvent
    data class TargetSelected(val format: DocumentFormat) : ConverterEvent
    data object StartConversion : ConverterEvent
    data object DismissError : ConverterEvent
    data object Reset : ConverterEvent
}
