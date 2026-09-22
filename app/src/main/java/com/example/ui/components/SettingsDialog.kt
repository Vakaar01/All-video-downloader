package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurface
import com.example.ui.theme.NeonGreen

@Composable
fun SettingsDialog(
    currentApiKey: String,
    isAlwaysOnEnabled: Boolean,
    isVoiceOutputEnabled: Boolean,
    currentPitch: Float,
    currentRate: Float,
    onSave: (apiKey: String, alwaysOn: Boolean, voiceOutput: Boolean, pitch: Float, rate: Float) -> Unit,
    onDismiss: () -> Unit,
    onTestVoice: (pitch: Float, rate: Float) -> Unit
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey) }
    var alwaysOnState by remember { mutableStateOf(isAlwaysOnEnabled) }
    var voiceOutputState by remember { mutableStateOf(isVoiceOutputEnabled) }
    var pitchState by remember { mutableFloatStateOf(currentPitch) }
    var rateState by remember { mutableFloatStateOf(currentRate) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("settings_dialog_surface"),
            shape = RoundedCornerShape(16.dp),
            color = HudSurface,
            border = BorderStroke(1.dp, CyanPrimary)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "⚙️ JARVIS PROTOCOL SETTINGS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini API Key Input
                Text(
                    text = "GEMINI AI API KEY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Supports Gemini 3.5 Flash for high-intelligence reasoning.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    placeholder = { Text("Enter AI Studio API Key", color = Color.Gray, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = HudBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Always-On Persistent Background Service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ALWAYS-ON SERVICE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Runs in foreground to prevent OS termination and maintain device standby.",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = alwaysOnState,
                        onCheckedChange = { alwaysOnState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen
                        ),
                        modifier = Modifier.testTag("always_on_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Speech Output Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "JARVIS VOICE OUTPUT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Speaks responses aloud via speech synthesis.",
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = voiceOutputState,
                        onCheckedChange = { voiceOutputState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanPrimary
                        ),
                        modifier = Modifier.testTag("voice_output_switch")
                    )
                }

                if (voiceOutputState) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "VOICE PITCH: ${Math.round(pitchState * 100) / 100f}x",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = CyanPrimary
                    )
                    Slider(
                        value = pitchState,
                        onValueChange = { pitchState = it },
                        valueRange = 0.6f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary
                        )
                    )

                    Text(
                        text = "SPEECH RATE: ${Math.round(rateState * 100) / 100f}x",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = CyanPrimary
                    )
                    Slider(
                        value = rateState,
                        onValueChange = { rateState = it },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary
                        )
                    )

                    Button(
                        onClick = { onTestVoice(pitchState, rateState) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162A45)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TEST JARVIS VOICE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyanPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "CANCEL",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(apiKeyText.trim(), alwaysOnState, voiceOutputState, pitchState, rateState)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF041E28)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text(
                            text = "SAVE & SYNC",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
