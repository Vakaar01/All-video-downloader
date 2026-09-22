package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberGreenDark
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CyberDownloaderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val url by viewModel.url.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val isPlatformPickerOpen by viewModel.isPlatformPickerOpen.collectAsStateWithLifecycle()
    val isVaultOpen by viewModel.isVaultOpen.collectAsStateWithLifecycle()
    val isExtracting by viewModel.isExtracting.collectAsStateWithLifecycle()
    val extractProgress by viewModel.extractProgress.collectAsStateWithLifecycle()
    val terminalStatus by viewModel.terminalStatus.collectAsStateWithLifecycle()
    val isExtractionComplete by viewModel.isExtractionComplete.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadSpeedStatus by viewModel.downloadSpeedStatus.collectAsStateWithLifecycle()
    val lastDownloadedFile by viewModel.lastDownloadedFile.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            VakaarHeader(
                vaultItemCount = allDownloads.size,
                onOpenVault = { viewModel.onOpenVault() }
            )
        },
        containerColor = CyberBlack,
        modifier = modifier
            .fillMaxSize()
            .testTag("cyber_downloader_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Subtle cyber grid lines in background
                    val step = 48.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = Color(0x0A00FF66),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color(0x0A00FF66),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cyber HUD banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STATUS: ENCRYPTED // BYPASS READY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberGreen,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Text(
                        text = "DESTINATION: /sdcard/vakaar/",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }

                // 1. URL Input Box (Top of workflow)
                CyberTerminalInput(
                    url = url,
                    onUrlChange = { viewModel.onUrlChanged(it) },
                    onPasteClicked = { viewModel.onPasteFromClipboard() },
                    onClearClicked = { viewModel.onClearUrl() }
                )

                // 2. Category Selector (Directly above the center download button)
                ActivePlatformCard(
                    platform = selectedPlatform,
                    onOpenChangeDialog = { viewModel.onOpenPlatformPicker() }
                )

                // 3. Center Download & Extract Button
                NeonDownloadButton(
                    isExtracting = isExtracting,
                    isComplete = isExtractionComplete,
                    onClick = { viewModel.onStartExtraction() }
                )

                // 4. Extraction Progress Line (0 to 100%)
                AnimatedVisibility(
                    visible = isExtracting || isExtractionComplete,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    CyberProgressSection(
                        progress = extractProgress,
                        currentStepText = terminalStatus
                    )
                }

                // 5. Revealed Format Buttons (Original MP4, HD 1080p, MP3)
                AnimatedVisibility(
                    visible = isExtractionComplete,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    FormatSelectionCard(
                        onSelectFormat = { format -> viewModel.onDownloadFormat(format) },
                        isDownloading = isDownloading
                    )
                }

                // 6. Active Download State & File Persist Notification
                if (isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CutCornerShape(8.dp))
                            .background(CyberCardBg)
                            .border(BorderStroke(1.dp, CyberCyan), CutCornerShape(8.dp))
                            .padding(14.dp)
                            .testTag("downloading_status_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DOWNLOADING STREAM...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = CyberCyan,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "$downloadProgress%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = CyberGreen,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CyberBlack)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (downloadProgress.toFloat() / 100f).coerceIn(0f, 1f))
                                    .height(10.dp)
                                    .background(CyberGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = downloadSpeedStatus,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CyberYellow,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // 7. Last Saved File Success Banner
                if (lastDownloadedFile != null && !isDownloading) {
                    val file = lastDownloadedFile!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CutCornerShape(8.dp))
                            .background(CyberGreenDark.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, CyberGreen), CutCornerShape(8.dp))
                            .padding(12.dp)
                            .testTag("download_success_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = CyberGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SUCCESS: FILE PERSISTED",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = CyberGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "SAVED AS: ${file.filePath}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextPrimary,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Footer Info
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "/// VAKAAR CYBERNETICS • SECURE STREAM ENGINE ///",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }

    // Floating Platform Picker Modal
    if (isPlatformPickerOpen) {
        PlatformPickerModal(
            selectedPlatform = selectedPlatform,
            onSelect = { viewModel.onSelectPlatform(it) },
            onDismiss = { viewModel.onClosePlatformPicker() }
        )
    }

    // Saved Media Vault Modal
    if (isVaultOpen) {
        VaultModal(
            downloads = allDownloads,
            onDelete = { viewModel.onDeleteDownload(it) },
            onDismiss = { viewModel.onCloseVault() }
        )
    }
}
