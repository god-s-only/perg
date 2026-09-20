package com.perg.converter

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.perg.converter.presentation.batch.BatchScreen
import com.perg.converter.presentation.converter.ConverterScreen
import com.perg.converter.presentation.merge.MergeScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Converter)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Converter> {
          ConverterScreen(
            onMergeClick = { backStack.add(Merge) },
            onBatchClick = { backStack.add(Batch) },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Merge> {
          MergeScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<Batch> {
          BatchScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
      },
  )
}
