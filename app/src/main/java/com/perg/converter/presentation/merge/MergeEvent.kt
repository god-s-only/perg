package com.perg.converter.presentation.merge

sealed interface MergeEvent {
    data class SourcesPicked(val items: List<MergeSource>) : MergeEvent
    data class RemoveSource(val uri: String) : MergeEvent
    data class OutputNameChanged(val name: String) : MergeEvent
    data object StartMerge : MergeEvent
    data class ConfirmRename(val name: String) : MergeEvent
    data object DismissError : MergeEvent
    data object Reset : MergeEvent
}
