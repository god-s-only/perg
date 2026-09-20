package com.perg.converter.data.converter

import android.content.Context
import android.net.Uri
import com.perg.converter.domain.model.EditableDocument
import com.perg.converter.domain.model.EditableParagraph
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfEditor @Inject constructor() {
    suspend fun extract(context: Context, sourceUri: String): EditableDocument {
        return withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                PDDocument.load(input).use { doc ->
                    val paragraphs = mutableListOf<EditableParagraph>()
                    for (page in 0 until doc.numberOfPages) {
                        paragraphs.addAll(pageParagraphs(doc, page))
                    }
                    if (paragraphs.isEmpty()) throw IllegalArgumentException("NoTextFound")
                    EditableDocument(pageCount = doc.numberOfPages, paragraphs = paragraphs)
                }
            } ?: throw IllegalArgumentException("ReadFailed")
        }
    }

    suspend fun save(
        context: Context,
        sourceUri: String,
        edits: Map<String, String>,
        outputFile: File
    ) {
        withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            context.contentResolver.openInputStream(Uri.parse(sourceUri)).use { input ->
                PDDocument.load(input).use { doc ->
                    val paragraphs = mutableListOf<EditableParagraph>()
                    for (page in 0 until doc.numberOfPages) {
                        paragraphs.addAll(pageParagraphs(doc, page))
                    }
                    val byId = paragraphs.associateBy { it.id }
                    val byPage = edits.mapNotNull { (id, text) ->
                        val para = byId[id] ?: return@mapNotNull null
                        if (text == para.text) return@mapNotNull null
                        para to text
                    }.groupBy({ it.first.page }, { it })
                    for ((pageIndex, pairs) in byPage) {
                        val page = doc.getPage(pageIndex)
                        val pageHeight = page.mediaBox.height
                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            for ((para, newText) in pairs) {
                                drawReplacement(cs, para, newText, pageHeight)
                            }
                        }
                    }
                    outputFile.outputStream().use { out -> doc.save(out) }
                }
            } ?: throw IllegalArgumentException("ReadFailed")
        }
    }

    private fun pageParagraphs(doc: PDDocument, page: Int): List<EditableParagraph> {
        val collector = PositionCollector()
        collector.startPage = page + 1
        collector.endPage = page + 1
        collector.getText(doc)
        val lines = groupLines(collector.current)
        val out = mutableListOf<EditableParagraph>()
        var paraIndex = 0
        var current: MutableList<List<TextPosition>>? = null
        var prevY = 0f
        var prevSize = 0f
        var prevX = 0f
        fun flush() {
            current?.let {
                out.add(buildParagraph(page, paraIndex++, it))
                current = null
            }
        }
        for (line in lines) {
            val y = line.map { it.yDirAdj }.average().toFloat()
            val size = line.map { it.fontSize }.average().toFloat()
            val x = line.minOf { it.xDirAdj }
            val same = current != null && (y - prevY) <= max(prevSize, size) * 1.8f && abs(x - prevX) < 30f
            if (!same) flush()
            if (current == null) current = mutableListOf()
            current!!.add(line)
            prevY = y
            prevSize = size
            prevX = x
        }
        flush()
        return out.filter { it.text.isNotBlank() }
    }

    private fun groupLines(positions: List<TextPosition>): List<List<TextPosition>> {
        val sorted = positions.sortedWith(compareBy({ it.yDirAdj }, { it.xDirAdj }))
        val lines = mutableListOf<MutableList<TextPosition>>()
        for (p in sorted) {
            val line = lines.lastOrNull()
            val tol = max(p.heightDir * 0.5f, 1f)
            if (line != null && abs(p.yDirAdj - line.last().yDirAdj) <= tol) {
                line.add(p)
            } else {
                lines.add(mutableListOf(p))
            }
        }
        return lines.map { it.sortedBy { t -> t.xDirAdj } }
    }

    private fun buildParagraph(page: Int, index: Int, lines: List<List<TextPosition>>): EditableParagraph {
        val text = lines.joinToString(" ") { line -> line.joinToString("") { it.unicode } }.trim()
        val flat = lines.flatten()
        val x = flat.minOf { it.xDirAdj }
        val y = flat.map { it.yDirAdj }.average().toFloat()
        val width = flat.maxOf { it.xDirAdj + it.widthDirAdj } - x
        val height = flat.maxOf { it.heightDir }
        val size = flat.map { it.fontSize }.average().toFloat().coerceIn(6f, 72f)
        return EditableParagraph(
            id = "p" + page + "_" + index,
            page = page,
            text = text,
            x = x,
            y = y,
            width = width,
            height = height,
            fontSize = size
        )
    }

    private fun drawReplacement(
        cs: PDPageContentStream,
        para: EditableParagraph,
        newText: String,
        pageHeight: Float
    ) {
        val lines = newText.split("\n")
        val lineHeight = para.fontSize * 1.2f
        val pad = 2f
        val top = pageHeight - para.y + pad
        val coverHeight = max(para.height, lines.size * lineHeight) + pad * 2f
        cs.setNonStrokingColor(1f, 1f, 1f)
        cs.addRect(para.x - pad, top - coverHeight, para.width + pad * 2f, coverHeight)
        cs.fill()
        cs.beginText()
        cs.setFont(PDType1Font.HELVETICA, para.fontSize)
        cs.setNonStrokingColor(0f, 0f, 0f)
        cs.newLineAtOffset(para.x, top - pad - para.fontSize)
        cs.showText(lines.firstOrNull() ?: "")
        for (extra in lines.drop(1)) {
            cs.newLineAtOffset(0f, -lineHeight)
            cs.showText(extra)
        }
        cs.endText()
    }

    private class PositionCollector : PDFTextStripper() {
        val current = mutableListOf<TextPosition>()
        override fun processTextPosition(text: TextPosition) {
            super.processTextPosition(text)
            current.add(text)
        }
    }
}
