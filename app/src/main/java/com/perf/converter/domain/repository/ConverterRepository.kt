package com.perf.converter.domain.repository

import com.perf.converter.domain.model.ConversionJob
import kotlinx.coroutines.flow.Flow

interface ConverterRepository {
    fun convert(job: ConversionJob): Flow<ConversionJob>
}
