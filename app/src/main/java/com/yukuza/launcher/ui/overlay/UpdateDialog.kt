package com.yukuza.launcher.ui.overlay

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yukuza.launcher.R
import com.yukuza.launcher.domain.model.UpdateInfo
import com.yukuza.launcher.ui.components.glass.GlassCard
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    var downloadId by remember { mutableLongStateOf(-1L) }

    DisposableEffect(downloadId) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId != downloadId) return

                val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
                val query = DownloadManager.Query().setFilterById(completedId)
                val cursor = dm.query(query)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val statusCol = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = it.getInt(statusCol)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val apkFile = File(
                                ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                "yukuza-update.apk",
                            )
                            val apkUri = FileProvider.getUriForFile(
                                ctx,
                                "${ctx.packageName}.fileprovider",
                                apkFile,
                            )
                            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                                data = apkUri
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(installIntent)
                        } else {
                            Toast.makeText(ctx, ctx.getString(R.string.update_toast_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                isDownloading = false
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        GlassCard(
            Modifier
                .fillMaxWidth(0.5f)
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.update_dialog_version, updateInfo.latestVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 6,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            startDownload(
                                context = context,
                                url = updateInfo.downloadUrl,
                                onDownloadStarted = { id ->
                                    downloadId = id
                                    isDownloading = true
                                },
                            )
                        },
                        enabled = !isDownloading,
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.update_dialog_install))
                    }
                    OutlinedButton(onClick = onDismiss, enabled = !isDownloading) {
                        Text(stringResource(R.string.update_dialog_dismiss), color = Color.White)
                    }
                }
            }
        }
    }
}

private fun startDownload(
    context: Context,
    url: String,
    onDownloadStarted: (Long) -> Unit,
) {
    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    if (dm == null) {
        Toast.makeText(context, context.getString(R.string.update_toast_unavailable), Toast.LENGTH_SHORT).show()
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        Toast.makeText(context, context.getString(R.string.update_toast_no_permission), Toast.LENGTH_LONG).show()
        return
    }
    val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "yukuza-update.apk")
    apkFile.delete()
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Yukuza Launcher update")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        .setDestinationUri(Uri.fromFile(apkFile))
    val id = dm.enqueue(request)
    onDownloadStarted(id)
}
