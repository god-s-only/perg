package com.perg.converter.domain.usecase

import com.perg.converter.domain.model.ConversionCapabilities
import com.perg.converter.domain.model.ConversionJob
import com.perg.converter.domain.model.ConversionStatus
import com.perg.converter.domain.repository.ConverterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ConvertFileUseCase @Inject constructor(private val repository: ConverterRepository) {
    operator fun invoke(job: ConversionJob): Flow<ConversionJob> {
        if (!ConversionCapabilities.isSupported(job.sourceFormat, job.targetFormat)) {
            return flow {
                emit(job.copy(status = ConversionStatus.FAILED, error = "UnsupportedConversion"))
            }
        }
        return repository.convert(job)
    }
}
