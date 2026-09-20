package com.perg.converter.presentation.batch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perg.converter.domain.model.ConversionCapabilities
import com.perg.converter.domain.model.DocumentFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val items = uris.mapNotNull { uri ->
            val name = displayName(context, uri) ?: return@mapNotNull null
            val format = DocumentFormat.fromFileName(name) ?: return@mapNotNull null
            if (ConversionCapabilities.targetsFor(format).isEmpty()) return@mapNotNull null
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
            }
            PickedFile(uri.toString(), name, format)
        }
        if (items.isNotEmpty()) viewModel.onEvent(BatchEvent.FilesPicked(items))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch convert") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Files", style = MaterialTheme.typography.labelLarge)
                    if (state.items.isEmpty()) {
                        Text("Pick images, PDFs or text files. Unsupported types are skipped.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(180.dp)) {
                            items(state.items, key = { it.uri }) { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            batchStatusText(item),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (!state.isConverting) {
                                        IconButton(onClick = { viewModel.onEvent(BatchEvent.RemoveFile(item.uri)) }) {
                                            Text("X")
                                        }
                                    }
                                }
                                if (item.status == BatchStatus.RUNNING) {
                                    LinearProgressIndicator(
                                        progress = { item.progress / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                        Text(if (state.items.isEmpty()) "Pick files" else "Add more")
                    }
                }
            }
            if (state.availableTargets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Convert all to", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.availableTargets.forEach { target ->
                            FilterChip(
                                selected = state.targetFormat == target,
                                onClick = { viewModel.onEvent(BatchEvent.TargetSelected(target)) },
                                label = { Text(target.name) }
                            )
                        }
                    }
                }
            } else if (state.items.isNotEmpty()) {
                Text("These files share no common output format.", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { viewModel.onEvent(BatchEvent.StartBatch) },
                enabled = state.items.isNotEmpty() && state.targetFormat != null && !state.isConverting,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (state.isConverting) "Converting..." else "Convert ${state.items.size} files") }
            Text("Files save to Download / Perg.", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (state.showSummary && state.finished) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BatchEvent.Reset) },
            title = { Text("Batch complete") },
            text = { Text("${state.doneCount} of ${state.items.size} saved to Download / Perg") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BatchEvent.Reset) }) { Text("Close") }
            }
        )
    }
}

private fun batchStatusText(item: BatchItem): String {
    return when (item.status) {
        BatchStatus.QUEUED -> item.sourceFormat.name
        BatchStatus.RUNNING -> "${item.progress}%"
        BatchStatus.DONE -> "Done"
        BatchStatus.FAILED -> item.error ?: "Failed"
    }
}

private fun displayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }
    } catch (e: Exception) {
        null
    }
}
