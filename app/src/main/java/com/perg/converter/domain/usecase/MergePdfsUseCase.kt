package com.perg.converter.domain.usecase

import com.perg.converter.domain.model.ConversionStatus
import com.perg.converter.domain.model.MergeJob
import com.perg.converter.domain.repository.ConverterRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MergePdfsUseCase @Inject constructor(private val repository: ConverterRepository) {
    operator fun invoke(sourceUris: List<String>, outputName: String): Flow<MergeJob> {
        if (sourceUris.size < 2) {
            return flow {
                emit(MergeJob(total = sourceUris.size, status = ConversionStatus.FAILED, error = "NeedTwoPdfs"))
            }
        }
        if (outputName.trim().isEmpty()) {
            return flow {
                emit(MergeJob(total = sourceUris.size, status = ConversionStatus.FAILED, error = "NameRequired"))
            }
        }
        return repository.mergePdfs(sourceUris, outputName.trim())
    }
}
