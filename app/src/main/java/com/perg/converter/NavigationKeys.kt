package com.perg.converter

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Converter : NavKey

@Serializable data object Merge : NavKey

@Serializable data object Batch : NavKey

@Serializable data object Editor : NavKey
