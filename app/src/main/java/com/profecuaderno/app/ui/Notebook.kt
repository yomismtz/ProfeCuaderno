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
                val gap = 28.dp.toPx()
                var y = gap
                while (y < size.height) {
                    drawLine(Color(0xFFE5E0EE), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                    y += gap
                }
                drawLine(
                    Color(0xFFE7A8A8),
                    start = androidx.compose.ui.geometry.Offset(42.dp.toPx(), 0f),
                    end = androidx.compose.ui.geometry.Offset(42.dp.toPx(), size.height),
                    strokeWidth = 2f
                )
            }
    ) { content() }
}

private val MaterialThemePaper = Color(0xFFFFFDF8)
