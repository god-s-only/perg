package com.perg.converter.presentation.converter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perg.converter.domain.model.ConversionCapabilities
import com.perg.converter.domain.model.ConversionJob
import com.perg.converter.domain.model.ConversionStatus
import com.perg.converter.domain.model.DocumentFormat
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
    private val convertFileUseCase: ConvertFileUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state.asStateFlow()
    private var convertJob: Job? = null

    fun onEvent(event: ConverterEvent) {
        when (event) {
            is ConverterEvent.SourcePicked -> onSourcePicked(event.uri, event.fileName)
            is ConverterEvent.TargetSelected -> onTargetSelected(event.format)
            ConverterEvent.StartConversion -> onStartConversion()
            ConverterEvent.DismissError -> _state.update { it.copy(error = null) }
            ConverterEvent.Reset -> onReset()
        }
    }

    private fun onSourcePicked(uri: String, fileName: String) {
        val format = DocumentFormat.fromFileName(fileName) ?: return
        _state.update {
            it.copy(
                sourceUri = uri,
                sourceFormat = format,
                targetFormat = null,
                availableTargets = ConversionCapabilities.targetsFor(format),
                outputUri = null,
                error = null
            )
        }
    }

    private fun onTargetSelected(format: DocumentFormat) {
        _state.update { it.copy(targetFormat = format, outputUri = null, error = null) }
    }

    private fun onStartConversion() {
        val current = _state.value
        val sourceUri = current.sourceUri ?: return
        val sourceFormat = current.sourceFormat ?: return
        val targetFormat = current.targetFormat ?: return
        if (current.isConverting) return
        convertJob?.cancel()
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
                        error = update.error
                    )
                }
            }
        }
    }

    private fun onReset() {
        convertJob?.cancel()
        _state.update { ConverterState() }
    }
}
