package com.perg.converter.presentation.merge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perg.converter.domain.model.DocumentFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MergeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val items = uris.mapNotNull { uri ->
            val name = displayName(context, uri) ?: return@mapNotNull null
            if (DocumentFormat.fromFileName(name) != DocumentFormat.PDF) return@mapNotNull null
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
            }
            MergeSource(uri.toString(), name)
        }
        if (items.isNotEmpty()) viewModel.onEvent(MergeEvent.SourcesPicked(items))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDFs") },
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
                    Text("1. PDF files in merge order", style = MaterialTheme.typography.labelLarge)
                    if (state.sources.isEmpty()) {
                        Text("Pick two or more PDFs. They merge top to bottom.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(180.dp)) {
                            items(state.sources, key = { it.uri }) { source ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        source.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.onEvent(MergeEvent.RemoveSource(source.uri)) }) {
                                        Text("X")
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                        Text(if (state.sources.isEmpty()) "Pick PDFs" else "Add more")
                    }
                }
            }
            OutlinedTextField(
                value = state.outputName,
                onValueChange = { viewModel.onEvent(MergeEvent.OutputNameChanged(it)) },
                label = { Text("Merged file name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.onEvent(MergeEvent.StartMerge) },
                enabled = state.sources.size >= 2 && !state.isMerging,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (state.isMerging) "Merging..." else "Merge ${state.sources.size} PDFs") }
            if (state.isMerging) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.weight(1f)
                    )
                    Text("${state.progress}%", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Merged file saves to Download / Perg.", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (state.outputUri != null) {
        var name by remember(state.outputUri) { mutableStateOf(state.mergedName ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(MergeEvent.Reset) },
            title = { Text("Merge complete") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Saved to Download / Perg")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("File name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.onEvent(MergeEvent.ConfirmRename(name)) }) {
                            Text("Save name")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (openMerged(context, state.outputUri!!)) viewModel.onEvent(MergeEvent.Reset)
                }) { Text("Open file") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(MergeEvent.Reset) }) { Text("Close") }
            }
        )
    }
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(MergeEvent.DismissError) },
            title = { Text("Merge failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(MergeEvent.DismissError) }) { Text("Close") }
            }
        )
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

private fun openMerged(context: Context, uri: String): Boolean {
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
