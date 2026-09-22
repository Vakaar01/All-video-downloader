package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.components.ArcReactorView
import com.example.ui.components.CommandTerminal
import com.example.ui.components.PermissionsHub
import com.example.ui.components.QuickActionGrid
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TelemetryBar
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.HudBackground
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                JarvisApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}

@Composable
fun JarvisApp(viewModel: JarvisViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val permissions by viewModel.permissionStatus.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val customApiKey by viewModel.customApiKey.collectAsState()
    val alwaysOnEnabled by viewModel.alwaysOnEnabled.collectAsState()
    val voiceOutputEnabled by viewModel.voiceOutputEnabled.collectAsState()
    val speechPitch by viewModel.speechPitch.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }

    // Multi-permission request launcher
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissions()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBackground),
        containerColor = HudBackground,
        topBar = {
            JarvisTopBar(
                isServiceRunning = isServiceRunning,
                onSettingsClick = { viewModel.openSettings() }
            )
        },
        bottomBar = {
            JarvisBottomInputBar(
                inputText = inputPrompt,
                onInputChange = { inputPrompt = it },
                onSend = {
                    if (inputPrompt.isNotBlank()) {
                        viewModel.sendUserMessage(inputPrompt)
                        inputPrompt = ""
                    }
                },
                isListening = isListening,
                isProcessing = isProcessing,
                onToggleListening = {
                    if (!permissions.hasRecordAudio) {
                        permissionLauncher.launch(permissionsToRequest)
                    } else {
                        viewModel.toggleListening()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Permission status & elevation hub
            AnimatedVisibility(visible = !permissions.isAllGranted) {
                PermissionsHub(
                    status = permissions,
                    onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) },
                    onRequestBatteryOptimization = { viewModel.requestIgnoreBatteryOptimization() }
                )
            }

            // 2. Central Holographic Arc Reactor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ArcReactorView(
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        audioRms = audioRms,
                        onClick = {
                            if (!permissions.hasRecordAudio) {
                                permissionLauncher.launch(permissionsToRequest)
                            } else {
                                viewModel.toggleListening()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            isListening -> "● LISTENING TO AUDIO STREAM..."
                            isSpeaking -> "● TRANSMITTING AUDIO RESPONSE..."
                            isProcessing -> "● JARVIS BRAIN REASONING..."
                            else -> "TAP REACTOR OR MIC TO ENGAGE"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isListening -> AmberAccent
                            isSpeaking -> CyanGlow
                            isProcessing -> NeonGreen
                            else -> CyanPrimary.copy(alpha = 0.8f)
                        },
                        letterSpacing = 1.sp
                    )
                }
            }

            // 3. Real-Time Telemetry status
            TelemetryBar(
                telemetry = telemetry,
                isServiceRunning = isServiceRunning,
                onFlashlightToggle = {
                    if (!permissions.hasCamera) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    } else {
                        viewModel.toggleFlashlight()
                    }
                }
            )

            // 4. Mark LIII Fast Action Dispatch
            QuickActionGrid(
                onActionClick = { cmd ->
                    viewModel.sendUserMessage(cmd)
                }
            )

            // 5. Communication Terminal & Log
            CommandTerminal(
                messages = messages,
                modifier = Modifier.height(260.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (isSettingsOpen) {
        SettingsDialog(
            currentApiKey = customApiKey,
            isAlwaysOnEnabled = alwaysOnEnabled,
            isVoiceOutputEnabled = voiceOutputEnabled,
            currentPitch = speechPitch,
            currentRate = speechRate,
            onSave = { key, alwaysOn, voice, pitch, rate ->
                viewModel.saveSettings(key, alwaysOn, voice, pitch, rate)
            },
            onDismiss = { viewModel.closeSettings() },
            onTestVoice = { pitch, rate ->
                viewModel.testVoice(pitch, rate)
            }
        )
    }
}

@Composable
fun JarvisTopBar(
    isServiceRunning: Boolean,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = HudSurface,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, HudBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning) NeonGreen else AmberAccent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "J.A.R.V.I.S.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "MARK LIII // ANDROID CORE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("topbar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Protocol Settings",
                    tint = CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun JarvisBottomInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    isProcessing: Boolean,
    onToggleListening: () -> Unit
) {
    Surface(
        color = HudSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HudBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice activation button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) AmberAccent else Color(0xFF162A45)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isListening) AmberAccent else CyanPrimary,
                        shape = CircleShape
                    )
                    .clickable(onClick = onToggleListening)
                    .testTag("voice_listen_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isListening) Color.Black else CyanPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Command input field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = if (isListening) "Listening to voice..." else "Command JARVIS...",
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = HudBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyanPrimary
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send or Processing button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank()) CyanPrimary else Color(0xFF162A45)
                    )
                    .clickable(
                        enabled = inputText.isNotBlank() && !isProcessing,
                        onClick = onSend
                    )
                    .testTag("command_send_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CyanPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Execute Command",
                        tint = if (inputText.isNotBlank()) Color(0xFF041E28) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
