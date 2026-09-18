package com.perg.converter.domain.repository

import com.perg.converter.domain.model.ConversionJob
import kotlinx.coroutines.flow.Flow

interface ConverterRepository {
    fun convert(job: ConversionJob): Flow<ConversionJob>
}
