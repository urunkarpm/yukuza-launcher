package com.yukuza.launcher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

fun glassGIcon(): ImageVector = ImageVector.Builder(
    name = "GlassG",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = SolidColor(Color.White.copy(alpha = 0.85f)),
        pathFillType = PathFillType.NonZero,
    ) {
        // Draw the Google G letterform outline
        moveTo(12f, 2f)
        arcTo(10f, 10f, 0f, false, false, 2f, 12f)
        arcTo(10f, 10f, 0f, false, false, 12f, 22f)
        arcTo(10f, 10f, 0f, false, false, 22f, 12f)
        lineTo(22f, 11f)
        lineTo(12f, 11f)
        lineTo(12f, 13f)
        lineTo(19.9f, 13f)
        arcTo(8f, 8f, 0f, false, true, 12f, 20f)
        arcTo(8f, 8f, 0f, false, true, 4f, 12f)
        arcTo(8f, 8f, 0f, false, true, 12f, 4f)
        arcTo(8f, 8f, 0f, false, true, 17.66f, 6.34f)
        lineTo(19.07f, 4.93f)
        arcTo(10f, 10f, 0f, false, false, 12f, 2f)
        close()
    }
}.build()
