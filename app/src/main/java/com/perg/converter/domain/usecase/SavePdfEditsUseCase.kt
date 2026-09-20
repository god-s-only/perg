package com.perg.converter.domain.usecase

import com.perg.converter.domain.repository.ConverterRepository
import javax.inject.Inject

class SavePdfEditsUseCase @Inject constructor(private val repository: ConverterRepository) {
    suspend operator fun invoke(sourceUri: String, edits: Map<String, String>, outputName: String): String {
        if (edits.isEmpty()) throw IllegalArgumentException("NoEdits")
        val name = outputName.trim()
        if (name.isEmpty()) throw IllegalArgumentException("NameRequired")
        return repository.saveDocumentEdits(sourceUri, edits, name)
    }
}
