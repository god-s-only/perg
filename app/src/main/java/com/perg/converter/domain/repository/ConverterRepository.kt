package com.perg.converter.domain.repository

import com.perg.converter.domain.model.ConversionJob
import com.perg.converter.domain.model.MergeJob
import kotlinx.coroutines.flow.Flow

interface ConverterRepository {
    fun convert(job: ConversionJob): Flow<ConversionJob>
    fun mergePdfs(sourceUris: List<String>, outputName: String): Flow<MergeJob>
    suspend fun rename(outputUri: String, displayName: String)
}
