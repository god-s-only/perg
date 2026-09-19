package com.perg.converter.domain.model

data class MergeJob(
    val total: Int,
    val merged: Int = 0,
    val status: ConversionStatus = ConversionStatus.IDLE,
    val progress: Int = 0,
    val outputUri: String? = null,
    val error: String? = null
)
