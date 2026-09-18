package com.perg.converter

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.perg.converter.presentation.converter.ConverterScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Converter)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Converter> {
          ConverterScreen(modifier = Modifier.safeDrawingPadding().padding(16.dp))
        }
      },
  )
}
