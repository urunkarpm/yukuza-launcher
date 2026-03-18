package com.yukuza.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val YukuzaTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.W200,
        fontSize = 72.sp,
        letterSpacing = 5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        letterSpacing = 3.sp,
    ),
)
