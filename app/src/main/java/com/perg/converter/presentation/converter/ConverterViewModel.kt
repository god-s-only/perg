package com.perg.converter.presentation.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perg.converter.domain.model.ConversionCapabilities
import com.perg.converter.domain.model.ConversionJob
import com.perg.converter.domain.model.ConversionStatus
import com.perg.converter.domain.model.DocumentFormat
import com.perg.converter.domain.repository.ConverterRepository
import com.perg.converter.domain.usecase.ConvertFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConverterViewModel @Inject constructor(
    private val convertFileUseCase: ConvertFileUseCase,
    private val repository: ConverterRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state.asStateFlow()
    private var convertJob: Job? = null
    private var pendingDisplayName: String? = null

    fun onEvent(event: ConverterEvent) {
        when (event) {
            is ConverterEvent.SourcePicked -> onSourcePicked(event.uri, event.fileName)
            is ConverterEvent.TargetSelected -> onTargetSelected(event.format)
            ConverterEvent.StartConversion -> onStartConversion()
            is ConverterEvent.ConfirmRename -> onConfirmRename(event.name)
            ConverterEvent.DismissRenameSuccess -> _state.update { it.copy(showRenameSuccess = false) }
            ConverterEvent.DismissError -> _state.update { it.copy(error = null) }
            ConverterEvent.Reset -> onReset()
        }
    }

    private fun onSourcePicked(uri: String, fileName: String) {
        val format = DocumentFormat.fromFileName(fileName) ?: return
        _state.update {
            it.copy(
                sourceUri = uri,
                sourceName = fileName,
                sourceFormat = format,
                targetFormat = null,
                availableTargets = ConversionCapabilities.targetsFor(format),
                outputUri = null,
                outputName = null,
                error = null
            )
        }
    }

    private fun onTargetSelected(format: DocumentFormat) {
        _state.update { it.copy(targetFormat = format, outputUri = null, outputName = null, error = null) }
    }

    private fun onStartConversion() {
        val current = _state.value
        val sourceUri = current.sourceUri ?: return
        val sourceFormat = current.sourceFormat ?: return
        val targetFormat = current.targetFormat ?: return
        if (current.isConverting) return
        convertJob?.cancel()
        pendingDisplayName = baseName(current.sourceName) + "." + targetFormat.extension
        val job = ConversionJob(
            id = UUID.randomUUID().toString(),
            sourceUri = sourceUri,
            sourceFormat = sourceFormat,
            targetFormat = targetFormat,
            status = ConversionStatus.QUEUED
        )
        convertJob = viewModelScope.launch {
            convertFileUseCase(job).collect { update ->
                _state.update {
                    it.copy(
                        isConverting = update.status == ConversionStatus.QUEUED || update.status == ConversionStatus.RUNNING,
                        progress = update.progress,
                        outputUri = update.outputUri,
                        outputName = if (update.status == ConversionStatus.SUCCEEDED) pendingDisplayName else it.outputName,
                        error = update.error
                    )
                }
            }
        }
    }

    private fun onReset() {
        convertJob?.cancel()
        pendingDisplayName = null
        _state.update { ConverterState() }
    }

    private fun onConfirmRename(name: String) {
        val uri = _state.value.outputUri ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val ext = _state.value.targetFormat?.extension
        val finalName = if (ext != null && !trimmed.endsWith("." + ext, ignoreCase = true)) trimmed + "." + ext else trimmed
        viewModelScope.launch {
            try {
                repository.rename(uri, finalName)
                pendingDisplayName = finalName
                _state.update { it.copy(outputName = finalName, showRenameSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun baseName(fileName: String?): String {
        val full = fileName ?: return "file"
        return full.substringBeforeLast('.').ifEmpty { "file" }
    }
}
