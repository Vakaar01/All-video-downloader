package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaFormat
import com.example.data.PlatformType
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanGlow
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberGreenDark
import com.example.ui.theme.CyberGreenGlow
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VakaarHeader(
    vaultItemCount: Int,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Surface(
        color = CyberBlack,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = CyberGreen.copy(alpha = 0.4f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo "VAKAAR"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("vakaar_brand_logo")
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CutCornerShape(8.dp))
                        .background(CyberCardBg)
                        .border(BorderStroke(1.5.dp, CyberGreen), CutCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Hacker Terminal Icon",
                        tint = CyberGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VAKAAR",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 22.sp,
                                letterSpacing = 3.sp,
                                color = CyberGreen
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberGreen.copy(alpha = glowAlpha))
                        )
                    }
                    Text(
                        text = "CYBER EXTRACTOR // v4.0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            color = CyberCyan
                        )
                    )
                }
            }

            // Corner Button for Vault / Downloads Screen
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(6.dp))
                    .background(CyberCardBg)
                    .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f)), CutCornerShape(6.dp))
                    .clickable { onOpenVault() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("corner_vault_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (vaultItemCount > 0) {
                                Badge(
                                    containerColor = CyberGreen,
                                    contentColor = Color.Black
                                ) {
                                    Text(
                                        text = "$vaultItemCount",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Open Saved Files Vault",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VAULT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CyberTerminalInput(
    url: String,
    onUrlChange: (String) -> Unit,
    onPasteClicked: () -> Unit,
    onClearClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(CyberCardBg)
            .border(BorderStroke(1.dp, CyberCardBorder), CutCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> TARGET_URL_STREAM",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberGreen,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "[HTTPS ONLY]",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = {
                Text(
                    text = "Paste media link here (Reels, YT, FB...)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("url_input_field"),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberGreen,
                unfocusedBorderColor = CyberCardBorder,
                focusedContainerColor = CyberBlack,
                unfocusedContainerColor = CyberBlack,
                cursorColor = CyberGreen
            ),
            shape = RoundedCornerShape(6.dp),
            trailingIcon = {
                Row {
                    if (url.isNotEmpty()) {
                        IconButton(
                            onClick = onClearClicked,
                            modifier = Modifier.testTag("btn_clear_url")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear input",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onPasteClicked,
                        modifier = Modifier.testTag("btn_paste_url")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste from clipboard",
                            tint = CyberGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun ActivePlatformCard(
    platform: PlatformType,
    onOpenChangeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(CyberCardBg)
            .border(BorderStroke(1.dp, platform.color.copy(alpha = 0.5f)), CutCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SELECTED PLATFORM",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            )

            // Button to open floating platform picker
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .background(CyberBlack)
                    .border(BorderStroke(1.dp, CyberCyan), CutCornerShape(4.dp))
                    .clickable { onOpenChangeDialog() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("btn_change_category")
            ) {
                Text(
                    text = "CHANGE ▾",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(platform.color.copy(alpha = 0.15f))
                    .border(1.dp, platform.color, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = platform.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = platform.color,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = platform.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = platform.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun NeonDownloadButton(
    isExtracting: Boolean,
    isComplete: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neonButton")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBorder"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isComplete -> CyberCyan
            isExtracting -> CyberMagenta
            else -> CyberGreen
        },
        label = "btnColor"
    )

    val glowColor = when {
        isComplete -> CyberCyanGlow
        isExtracting -> CyberMagenta
        else -> CyberGreenGlow
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(CutCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        buttonColor.copy(alpha = 0.2f),
                        CyberBlack,
                        buttonColor.copy(alpha = 0.2f)
                    )
                )
            )
            .border(
                BorderStroke(pulseBorder.dp, buttonColor),
                CutCornerShape(12.dp)
            )
            .clickable(enabled = !isExtracting) { onClick() }
            .testTag("btn_center_download"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when {
                    isComplete -> Icons.Default.CheckCircle
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = buttonColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    isComplete -> "STREAM DECRYPTED // READY"
                    isExtracting -> "EXTRACTING MEDIA..."
                    else -> "INITIALIZE EXTRACT & DOWNLOAD"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    color = buttonColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.2.sp
                )
            )
        }
    }
}

@Composable
fun CyberProgressSection(
    progress: Int,
    currentStepText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CutCornerShape(8.dp))
            .background(CyberCardBg)
            .border(BorderStroke(1.dp, CyberCardBorder), CutCornerShape(8.dp))
            .padding(14.dp)
            .testTag("cyber_progress_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXTRACTION PIPELINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberCyan,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented / Continuous Neon Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CyberBlack)
                .border(BorderStroke(1.dp, CyberGreen.copy(alpha = 0.4f)), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (progress.toFloat() / 100f).coerceIn(0f, 1f))
                    .height(12.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(CyberGreenDark, CyberGreen, CyberCyan)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Log Line
        Text(
            text = "> $currentStepText",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = CyberYellow,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            ),
            maxLines = 1
        )
    }
}

@Composable
fun FormatSelectionCard(
    onSelectFormat: (MediaFormat) -> Unit,
    isDownloading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CutCornerShape(10.dp))
            .background(CyberCardBg)
            .border(BorderStroke(1.5.dp, CyberCyan), CutCornerShape(10.dp))
            .padding(14.dp)
            .testTag("format_selection_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[+] SELECT TARGET FORMAT",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Text(
                text = "SAVED TO /sdcard/vakaar/",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = CyberGreen,
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        MediaFormat.entries.forEach { format ->
            FormatItemRow(
                format = format,
                isDownloading = isDownloading,
                onSelect = { onSelectFormat(format) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FormatItemRow(
    format: MediaFormat,
    isDownloading: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (format.isAudio) CyberYellow else CyberGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CyberBlack)
            .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
            .clickable(enabled = !isDownloading) { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("btn_format_${format.extension}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = format.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
            Text(
                text = format.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(CutCornerShape(4.dp))
                .background(borderColor.copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, borderColor), CutCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "DOWNLOAD",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = borderColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}
