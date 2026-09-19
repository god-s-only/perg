package com.perg.converter.presentation.merge

data class MergeSource(
    val uri: String,
    val name: String
)

data class MergeState(
    val sources: List<MergeSource> = emptyList(),
    val outputName: String = "merged",
    val isMerging: Boolean = false,
    val progress: Int = 0,
    val outputUri: String? = null,
    val mergedName: String? = null,
    val error: String? = null
)
