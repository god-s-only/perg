package com.perg.converter.presentation.merge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perg.converter.domain.model.ConversionStatus
import com.perg.converter.domain.repository.ConverterRepository
import com.perg.converter.domain.usecase.MergePdfsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MergeViewModel @Inject constructor(
    private val mergePdfsUseCase: MergePdfsUseCase,
    private val repository: ConverterRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MergeState())
    val state: StateFlow<MergeState> = _state.asStateFlow()
    private var mergeJob: Job? = null

    fun onEvent(event: MergeEvent) {
        when (event) {
            is MergeEvent.SourcesPicked -> onSourcesPicked(event.items)
            is MergeEvent.RemoveSource -> onRemoveSource(event.uri)
            is MergeEvent.OutputNameChanged -> _state.update { it.copy(outputName = event.name) }
            MergeEvent.StartMerge -> onStartMerge()
            is MergeEvent.ConfirmRename -> onConfirmRename(event.name)
            MergeEvent.DismissError -> _state.update { it.copy(error = null) }
            MergeEvent.Reset -> onReset()
        }
    }

    private fun onSourcesPicked(items: List<MergeSource>) {
        _state.update { current ->
            val known = current.sources.map { it.uri }.toSet()
            current.copy(
                sources = current.sources + items.filter { it.uri !in known },
                outputUri = null,
                mergedName = null,
                error = null
            )
        }
    }

    private fun onRemoveSource(uri: String) {
        _state.update { it.copy(sources = it.sources.filterNot { s -> s.uri == uri }) }
    }

    private fun onStartMerge() {
        val current = _state.value
        if (current.isMerging) return
        mergeJob?.cancel()
        mergeJob = viewModelScope.launch {
            mergePdfsUseCase(current.sources.map { it.uri }, current.outputName).collect { update ->
                _state.update {
                    it.copy(
                        isMerging = update.status == ConversionStatus.QUEUED || update.status == ConversionStatus.RUNNING,
                        progress = update.progress,
                        outputUri = update.outputUri,
                        mergedName = if (update.status == ConversionStatus.SUCCEEDED) {
                            update.outputUri?.let { current.outputName.trim().withPdf() }
                        } else {
                            it.mergedName
                        },
                        error = update.error
                    )
                }
            }
        }
    }

    private fun onConfirmRename(name: String) {
        val uri = _state.value.outputUri ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.rename(uri, trimmed.withPdf())
                _state.update { it.copy(mergedName = trimmed.withPdf()) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun onReset() {
        mergeJob?.cancel()
        val name = _state.value.outputName
        _state.update { MergeState(outputName = name) }
    }

    private fun String.withPdf(): String {
        return if (endsWith(".pdf", ignoreCase = true)) this else this + ".pdf"
    }
}
