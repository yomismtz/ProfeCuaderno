package com.profecuaderno.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NotebookBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialThemePaper)
            .drawBehind {
                // Hoja rayada muy suave: sin margen rojo vertical.
                // Las líneas quedan como textura de fondo y no compiten con tarjetas o diálogos.
                val gap = 32.dp.toPx()
                var y = gap
                while (y < size.height) {
                    drawLine(
                        color = Color(0xFFF0EDF4),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 0.8f
                    )
                    y += gap
                }
            }
    ) { content() }
}

private val MaterialThemePaper = Color(0xFFFFFDF9)
