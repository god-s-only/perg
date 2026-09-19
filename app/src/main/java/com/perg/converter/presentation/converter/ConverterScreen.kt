package com.perg.converter.presentation.converter

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.perg.converter.domain.model.DocumentFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    onMergeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
            }
            var name = "input"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) name = cursor.getString(index)
            }
            viewModel.onEvent(ConverterEvent.SourcePicked(uri.toString(), name))
        }
    }
    LaunchedEffect(state.showRenameSuccess) {
        if (state.showRenameSuccess) {
            snackbarHostState.showSnackbar("Saved Successfully")
            viewModel.onEvent(ConverterEvent.DismissRenameSuccess)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Perg Converter", fontWeight = FontWeight.SemiBold)
                        Text("100% offline, files stay on your device", style = MaterialTheme.typography.bodySmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        ConverterContent(
            state = state,
            onPick = { picker.launch(arrayOf("*/*")) },
            onTarget = { viewModel.onEvent(ConverterEvent.TargetSelected(it)) },
            onConvert = { viewModel.onEvent(ConverterEvent.StartConversion) },
            onMergeClick = onMergeClick,
            modifier = Modifier.padding(padding)
        )
    }
    if (state.outputUri != null) {
        var name by remember(state.outputUri) { mutableStateOf(state.outputName ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ConverterEvent.Reset) },
            title = { Text("Conversion complete") },
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
                        TextButton(onClick = { viewModel.onEvent(ConverterEvent.ConfirmRename(name)) }) {
                            Text("Save name")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (openOutput(context, state.outputUri!!, state.targetFormat)) {
                        viewModel.onEvent(ConverterEvent.Reset)
                    }
                }) { Text("Open file") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ConverterEvent.Reset) }) { Text("Close") }
            }
        )
    }
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ConverterEvent.DismissError) },
            title = { Text("Conversion failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(ConverterEvent.DismissError) }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ConverterContent(
    state: ConverterState,
    onPick: () -> Unit,
    onTarget: (DocumentFormat) -> Unit,
    onConvert: () -> Unit,
    onMergeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Source file", style = MaterialTheme.typography.labelLarge)
                if (state.sourceName == null) {
                    Text("Pick an image, PDF or text file. It never leaves your device.")
                    OutlinedButton(onClick = onPick) { Text("Pick file") }
                } else {
                    Text(state.sourceName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        state.sourceFormat?.let { formatLabel(it) } ?: "",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = onPick) { Text("Change file") }
                }
            }
        }
        if (state.availableTargets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Convert to", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableTargets.forEach { target ->
                        FilterChip(
                            selected = state.targetFormat == target,
                            onClick = { onTarget(target) },
                            label = { Text(formatLabel(target)) }
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onConvert,
                enabled = state.sourceUri != null && state.targetFormat != null && !state.isConverting,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (state.isConverting) "Converting..." else "Convert") }
            if (state.isConverting) {
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
        }
        Text(
            "Files save to Download / Perg, always offline.",
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = onMergeClick) { Text("Or merge multiple PDFs") }
    }
}

private fun formatLabel(format: DocumentFormat): String {
    return when (format) {
        DocumentFormat.JPEG -> "JPEG image"
        DocumentFormat.PNG -> "PNG image"
        DocumentFormat.WEBP -> "WEBP image"
        DocumentFormat.PDF -> "PDF document"
        DocumentFormat.DOCX -> "Word document"
        DocumentFormat.TXT -> "Text file"
    }
}

private fun openOutput(context: Context, uri: String, target: DocumentFormat?): Boolean {
    return try {
        val mime = when (target) {
            DocumentFormat.PDF -> "application/pdf"
            DocumentFormat.PNG -> "image/png"
            DocumentFormat.JPEG, DocumentFormat.WEBP -> "image/jpeg"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(uri), mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Open file"))
        true
    } catch (e: Exception) {
        false
    }
}
