package com.perg.converter.domain.usecase

import com.perg.converter.domain.model.EditableDocument
import com.perg.converter.domain.repository.ConverterRepository
import javax.inject.Inject

class LoadEditablePdfUseCase @Inject constructor(private val repository: ConverterRepository) {
    suspend operator fun invoke(sourceUri: String): EditableDocument {
        return repository.loadEditableDocument(sourceUri)
    }
}
