package com.kotlinfoundation.koko.presentation.screens.shadercamspike

import androidx.compose.ui.Modifier

// SkSL/Skiko port follows once a Mac is available to test it — Android validates the
// pipeline (camera preview → live shader) first. See the KotlinConf-Ideen doc for status.
actual fun Modifier.liveDitherShaderEffect(): Modifier = this
