package com.perg.converter.presentation.editor

import com.perg.converter.domain.model.EditableParagraph

data class EditorState(
    val sourceUri: String? = null,
    val sourceName: String? = null,
    val isLoading: Boolean = false,
    val paragraphs: List<EditableParagraph> = emptyList(),
    val edits: Map<String, String> = emptyMap(),
    val editingId: String? = null,
    val outputName: String = "edited",
    val isSaving: Boolean = false,
    val outputUri: String? = null,
    val error: String? = null
)
