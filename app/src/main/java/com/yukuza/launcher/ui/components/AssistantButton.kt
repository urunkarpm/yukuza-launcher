package com.yukuza.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader as AndroidShader
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.ui.theme.glassGIcon

@Composable
fun AssistantButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(50.dp)
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    @Suppress("NewApi")
                    renderEffect = AndroidRenderEffect
                        .createBlurEffect(30f, 30f, AndroidShader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            }
            .background(Color(0x73080314), CircleShape)
            .border(
                width = if (density >= 2f) 0.5.dp else 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = CircleShape,
            )
            .clickable { launchAssistant(context) }
            .semantics { contentDescription = context.getString(com.yukuza.launcher.R.string.google_assistant_content_description) },
    ) {
        Icon(
            imageVector = glassGIcon(),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(28.dp),
        )
    }
}

private fun launchAssistant(context: Context) {
    val intents = listOf(
        Intent(Intent.ACTION_VOICE_COMMAND),
        Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE),
        Intent("com.google.android.googlequicksearchbox.VOICE_SEARCH_ACTIVITY"),
    )
    intents.firstOrNull { it.resolveActivity(context.packageManager) != null }
        ?.let { context.startActivity(it) }
        ?: Toast.makeText(context, context.getString(com.yukuza.launcher.R.string.voice_assistant_not_available), Toast.LENGTH_SHORT).show()
}
