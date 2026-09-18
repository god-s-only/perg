package com.perf.converter.domain.usecase

import com.perf.converter.domain.model.ConversionCapabilities
import com.perf.converter.domain.model.ConversionJob
import com.perf.converter.domain.model.ConversionStatus
import com.perf.converter.domain.repository.ConverterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ConvertFileUseCase(private val repository: ConverterRepository) {
    operator fun invoke(job: ConversionJob): Flow<ConversionJob> {
        if (!ConversionCapabilities.isSupported(job.sourceFormat, job.targetFormat)) {
            return flow {
                emit(job.copy(status = ConversionStatus.FAILED, error = "UnsupportedConversion"))
            }
        }
        return repository.convert(job)
    }
}
