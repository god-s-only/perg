package com.perg.converter.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perg.converter.domain.usecase.LoadEditablePdfUseCase
import com.perg.converter.domain.usecase.SavePdfEditsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val loadEditablePdfUseCase: LoadEditablePdfUseCase,
    private val savePdfEditsUseCase: SavePdfEditsUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var saveJob: Job? = null

    fun onEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.PdfPicked -> onPdfPicked(event.uri, event.fileName)
            is EditorEvent.EditParagraph -> _state.update { it.copy(editingId = event.id) }
            EditorEvent.DismissEdit -> _state.update { it.copy(editingId = null) }
            is EditorEvent.ConfirmEdit -> onConfirmEdit(event.id, event.text)
            is EditorEvent.OutputNameChanged -> _state.update { it.copy(outputName = event.name) }
            EditorEvent.SaveDocument -> onSaveDocument()
            EditorEvent.DismissError -> _state.update { it.copy(error = null) }
            EditorEvent.Reset -> onReset()
        }
    }

    private fun onPdfPicked(uri: String, fileName: String) {
        loadJob?.cancel()
        saveJob?.cancel()
        _state.update {
            EditorState(sourceUri = uri, sourceName = fileName, isLoading = true, outputName = baseName(fileName) + "_edited")
        }
        loadJob = viewModelScope.launch {
            try {
                val doc = loadEditablePdfUseCase(uri)
                _state.update { it.copy(isLoading = false, paragraphs = doc.paragraphs) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun onConfirmEdit(id: String, text: String) {
        _state.update { current ->
            val original = current.paragraphs.firstOrNull { it.id == id }?.text
            val edits = if (original == null || text == original) {
                current.edits - id
            } else {
                current.edits + (id to text)
            }
            current.copy(edits = edits, editingId = null)
        }
    }

    private fun onSaveDocument() {
        val current = _state.value
        val uri = current.sourceUri ?: return
        if (current.isSaving || current.edits.isEmpty()) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val output = savePdfEditsUseCase(uri, current.edits, current.outputName)
                _state.update { it.copy(isSaving = false, outputUri = output) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    private fun onReset() {
        loadJob?.cancel()
        saveJob?.cancel()
        _state.update { EditorState() }
    }

    private fun baseName(fileName: String): String {
        return fileName.substringBeforeLast('.').ifEmpty { "file" }
    }
}
