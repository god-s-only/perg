package com.perg.converter.presentation.converter

import com.perg.converter.domain.model.DocumentFormat

data class ConverterState(
    val sourceUri: String? = null,
    val sourceName: String? = null,
    val sourceFormat: DocumentFormat? = null,
    val targetFormat: DocumentFormat? = null,
    val availableTargets: List<DocumentFormat> = emptyList(),
    val isConverting: Boolean = false,
    val progress: Int = 0,
    val outputUri: String? = null,
    val outputName: String? = null,
    val error: String? = null
)
