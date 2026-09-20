package com.perg.converter.presentation.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perg.converter.domain.model.DocumentFormat
import com.perg.converter.domain.model.EditableParagraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
            }
            var name = "input.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) name = cursor.getString(index)
            }
            if (DocumentFormat.fromFileName(name) == DocumentFormat.PDF) {
                viewModel.onEvent(EditorEvent.PdfPicked(uri.toString(), name))
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit PDF") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.sourceUri == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pick a text PDF to edit. Tap any paragraph to rewrite it.")
                        OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                            Text("Pick PDF")
                        }
                    }
                }
            } else if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Reading ${state.sourceName}...")
            } else {
                Text(
                    state.sourceName ?: "",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    var lastPage = -1
                    state.paragraphs.forEach { para ->
                        if (para.page != lastPage) {
                            lastPage = para.page
                            item(key = "header_" + para.page) {
                                Text(
                                    "Page " + (para.page + 1),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        item(key = para.id) {
                            ParagraphRow(
                                paragraph = para,
                                edited = state.edits[para.id],
                                onClick = { viewModel.onEvent(EditorEvent.EditParagraph(para.id)) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.outputName,
                    onValueChange = { viewModel.onEvent(EditorEvent.OutputNameChanged(it)) },
                    label = { Text("Edited file name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.onEvent(EditorEvent.SaveDocument) },
                    enabled = state.edits.isNotEmpty() && !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(if (state.isSaving) "Saving..." else "Save ${state.edits.size} edits") }
            }
            Text("Edited file saves to Download / Perg.", style = MaterialTheme.typography.bodySmall)
        }
    }
    val editing = state.paragraphs.firstOrNull { it.id == state.editingId }
    if (editing != null) {
        var text by remember(editing.id) { mutableStateOf(state.edits[editing.id] ?: editing.text) }
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EditorEvent.DismissEdit) },
            title = { Text("Edit paragraph") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.ConfirmEdit(editing.id, text)) }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.DismissEdit) }) { Text("Cancel") }
            }
        )
    }
    if (state.outputUri != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EditorEvent.Reset) },
            title = { Text("Saved") },
            text = { Text("Edited PDF is in Download / Perg") },
            confirmButton = {
                TextButton(onClick = {
                    if (openEdited(context, state.outputUri!!)) viewModel.onEvent(EditorEvent.Reset)
                }) { Text("Open file") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.Reset) }) { Text("Close") }
            }
        )
    }
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(EditorEvent.DismissError) },
            title = { Text("Editor failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(EditorEvent.DismissError) }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ParagraphRow(
    paragraph: EditableParagraph,
    edited: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (edited != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = edited ?: paragraph.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (edited != null) Text("Edited", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun openEdited(context: Context, uri: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(uri), "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Open file"))
        true
    } catch (e: Exception) {
        false
    }
}
