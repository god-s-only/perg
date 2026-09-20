package com.perg.converter.presentation.batch

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
class BatchViewModel @Inject constructor(
    private val convertFileUseCase: ConvertFileUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(BatchState())
    val state: StateFlow<BatchState> = _state.asStateFlow()
    private var batchJob: Job? = null

    fun onEvent(event: BatchEvent) {
        when (event) {
            is BatchEvent.FilesPicked -> onFilesPicked(event.items)
            is BatchEvent.RemoveFile -> onRemoveFile(event.uri)
            is BatchEvent.TargetSelected -> onTargetSelected(event.format)
            BatchEvent.StartBatch -> onStartBatch()
            BatchEvent.Reset -> onReset()
        }
    }

    private fun onFilesPicked(items: List<PickedFile>) {
        _state.update { current ->
            val known = current.sources()
            val fresh = items.filter { it.uri !in known }.map {
                BatchItem(uri = it.uri, name = it.name, sourceFormat = it.format)
            }
            current.copy(items = current.items + fresh, started = false, showSummary = false)
                .withTargets()
        }
    }

    private fun onRemoveFile(uri: String) {
        _state.update { it.copy(items = it.items.filterNot { item -> item.uri == uri }).withTargets() }
    }

    private fun onTargetSelected(format: DocumentFormat) {
        _state.update { it.copy(targetFormat = format) }
    }

    private fun onStartBatch() {
        val current = _state.value
        val target = current.targetFormat ?: return
        if (current.isConverting || current.items.isEmpty()) return
        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            _state.update { it.copy(isConverting = true, started = true, showSummary = false) }
            for (item in _state.value.items) {
                val job = ConversionJob(
                    id = UUID.randomUUID().toString(),
                    sourceUri = item.uri,
                    sourceFormat = item.sourceFormat,
                    targetFormat = target,
                    status = ConversionStatus.QUEUED
                )
                convertFileUseCase(job).collect { update ->
                    _state.update { state ->
                        state.copy(
                            items = state.items.map { row ->
                                if (row.uri != item.uri) row
                                else row.copy(
                                    status = when (update.status) {
                                        ConversionStatus.SUCCEEDED -> BatchStatus.DONE
                                        ConversionStatus.FAILED -> BatchStatus.FAILED
                                        else -> BatchStatus.RUNNING
                                    },
                                    progress = update.progress,
                                    outputUri = update.outputUri ?: row.outputUri,
                                    error = update.error
                                )
                            }
                        )
                    }
                }
            }
            _state.update { it.copy(isConverting = false, showSummary = true) }
        }
    }

    private fun onReset() {
        batchJob?.cancel()
        _state.update { BatchState() }
    }

    private fun BatchState.sources(): Set<String> = items.map { it.uri }.toSet()

    private fun BatchState.withTargets(): BatchState {
        val formats = items.map { it.sourceFormat }.distinct()
        val common = if (formats.isEmpty()) {
            emptyList()
        } else {
            formats.map { ConversionCapabilities.targetsFor(it).toSet() }
                .reduce { acc, set -> acc.intersect(set) }
                .toList()
        }
        val target = targetFormat?.takeIf { it in common }
        return copy(availableTargets = common, targetFormat = target)
    }
}
