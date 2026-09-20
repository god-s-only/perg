package com.perg.converter.presentation.editor

sealed interface EditorEvent {
    data class PdfPicked(val uri: String, val fileName: String) : EditorEvent
    data class EditParagraph(val id: String) : EditorEvent
    data object DismissEdit : EditorEvent
    data class ConfirmEdit(val id: String, val text: String) : EditorEvent
    data class OutputNameChanged(val name: String) : EditorEvent
    data object SaveDocument : EditorEvent
    data object DismissError : EditorEvent
    data object Reset : EditorEvent
}
