package com.perg.converter.domain.repository

import com.perg.converter.domain.model.ConversionJob
import com.perg.converter.domain.model.EditableDocument
import com.perg.converter.domain.model.MergeJob
import kotlinx.coroutines.flow.Flow

interface ConverterRepository {
    fun convert(job: ConversionJob): Flow<ConversionJob>
    fun mergePdfs(sourceUris: List<String>, outputName: String): Flow<MergeJob>
    suspend fun loadEditableDocument(sourceUri: String): EditableDocument
    suspend fun saveDocumentEdits(sourceUri: String, edits: Map<String, String>, outputName: String): String
    suspend fun rename(outputUri: String, displayName: String)
}
