package com.perf.converter.presentation.converter

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ConverterScreen(
    modifier: Modifier = Modifier,
    viewModel: ConverterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            var name = "input"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) name = cursor.getString(index)
            }
            viewModel.onEvent(ConverterEvent.SourcePicked(uri.toString(), name))
        }
    }
    ConverterContent(
        state = state,
        onPick = { picker.launch(arrayOf("*/*")) },
        onTarget = { viewModel.onEvent(ConverterEvent.TargetSelected(it)) },
        onConvert = { viewModel.onEvent(ConverterEvent.StartConversion) },
        onDismissError = { viewModel.onEvent(ConverterEvent.DismissError) },
        onReset = { viewModel.onEvent(ConverterEvent.Reset) },
        modifier = modifier
    )
}

@Composable
private fun ConverterContent(
    state: ConverterState,
    onPick: () -> Unit,
    onTarget: (com.perf.converter.domain.model.DocumentFormat) -> Unit,
    onConvert: () -> Unit,
    onDismissError: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Offline Converter", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = onPick) { Text(if (state.sourceFormat == null) "Pick file" else "Picked: ${state.sourceFormat}") }
        if (state.availableTargets.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableTargets.forEach { target ->
                    FilterChip(
                        selected = state.targetFormat == target,
                        onClick = { onTarget(target) },
                        label = { Text(target.name) }
                    )
                }
            }
        }
        Button(
            onClick = onConvert,
            enabled = state.sourceUri != null && state.targetFormat != null && !state.isConverting,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.isConverting) "Converting ${state.progress}%" else "Convert") }
        if (state.isConverting) LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
        state.outputUri?.let { Text("Saved: $it", style = MaterialTheme.typography.bodySmall) }
        state.error?.let {
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onDismissError) { Text("Dismiss") }
        }
        if (state.outputUri != null || state.error != null) OutlinedButton(onClick = onReset) { Text("Reset") }
    }
}
