package com.perg.converter.domain.model

data class EditableParagraph(
    val id: String,
    val page: Int,
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val fontSize: Float
)

data class EditableDocument(
    val pageCount: Int,
    val paragraphs: List<EditableParagraph>
)
