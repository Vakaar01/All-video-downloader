package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.DownloadEntity
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultModal(
    downloads: List<DownloadEntity>,
    onDelete: (DownloadEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(CutCornerShape(12.dp))
                .border(BorderStroke(1.5.dp, CyberCyan), CutCornerShape(12.dp))
                .testTag("vault_screen_modal"),
            color = CyberBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_close_vault")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CyberCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "VAKAAR MEDIA VAULT",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = CyberCyan,
                                    letterSpacing = 1.sp,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "TARGET: /sdcard/Download/vakaar/",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberGreen,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(4.dp))
                            .background(CyberGreen.copy(alpha = 0.15f))
                            .border(1.dp, CyberGreen, CutCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${downloads.size} FILES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (downloads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "VAULT STORAGE EMPTY",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Downloaded files will appear here",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(downloads, key = { it.id }) { item ->
                            VaultItemCard(
                                item = item,
                                onDelete = { onDelete(item) },
                                onPlay = { playMedia(context, item) },
                                onShare = { shareMedia(context, item) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(6.dp))
                        .background(CyberCardBg)
                        .border(BorderStroke(1.dp, CyberCardBorder), CutCornerShape(6.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RETURN TO TERMINAL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = CyberCyan,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun VaultItemCard(
    item: DownloadEntity,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(item.timestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(CyberCardBg)
            .border(BorderStroke(1.dp, CyberCardBorder), CutCornerShape(8.dp))
            .padding(12.dp)
            .testTag("vault_card_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (item.isAudio) CyberYellow.copy(alpha = 0.2f) else CyberGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isAudio) Icons.Default.Audiotrack else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = if (item.isAudio) CyberYellow else CyberGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "${item.platformName} • ${item.formattedSize} • $dateText",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp).testTag("btn_delete_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Delete from vault",
                    tint = CyberRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "PATH: ${item.filePath}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontSize = 9.sp
            ),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .background(CyberBlack)
                    .border(BorderStroke(0.8.dp, CyberCyan), CutCornerShape(4.dp))
                    .clickable { onShare() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SHARE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .background(CyberGreen.copy(alpha = 0.2f))
                    .border(BorderStroke(0.8.dp, CyberGreen), CutCornerShape(4.dp))
                    .clickable { onPlay() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("btn_play_${item.id}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PLAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

private fun getPlayableUri(context: Context, item: DownloadEntity): Uri? {
    // 1. Check primary file path
    val primaryFile = File(item.filePath)
    if (primaryFile.exists() && primaryFile.length() > 0) {
        try {
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", primaryFile)
        } catch (_: Exception) {}
    }

    // 2. Check public Download/vakaar folder
    val fileName = primaryFile.name
    val publicFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "vakaar/$fileName"
    )
    if (publicFile.exists() && publicFile.length() > 0) {
        try {
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", publicFile)
        } catch (_: Exception) {}
    }

    // 3. Check app external files dir
    val appLocalFile = File(File(context.getExternalFilesDir(null), "vakaar"), fileName)
    if (appLocalFile.exists() && appLocalFile.length() > 0) {
        try {
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", appLocalFile)
        } catch (_: Exception) {}
    }

    // 4. Fallback if it's already a content:// URI string
    if (item.filePath.startsWith("content://")) {
        return Uri.parse(item.filePath)
    }

    return null
}

private fun playMedia(context: Context, item: DownloadEntity) {
    try {
        val uri = getPlayableUri(context, item)
        if (uri == null) {
            Toast.makeText(context, "File not accessible: ${item.filePath}", Toast.LENGTH_LONG).show()
            return
        }

        val mime = if (item.isAudio) "audio/*" else "video/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Play with Video Player").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No media player found. Install VLC or MX Player.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot play: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareMedia(context: Context, item: DownloadEntity) {
    try {
        val uri = getPlayableUri(context, item)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            val mime = if (item.isAudio) "audio/*" else "video/*"
            type = mime
            if (uri != null) {
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                putExtra(Intent.EXTRA_TEXT, "Media: ${item.title}\nSaved at: ${item.filePath}")
            }
            putExtra(Intent.EXTRA_SUBJECT, item.title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Media").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
