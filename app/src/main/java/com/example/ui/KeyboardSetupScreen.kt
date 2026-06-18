package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyboardSetupScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Live state checks
    var isEnabled by remember { mutableStateOf(isKeyboardEnabled(context)) }
    var isSelected by remember { mutableStateOf(isKeyboardSelected(context)) }

    // Manual refresh check
    val refreshStates = {
        isEnabled = isKeyboardEnabled(context)
        isSelected = isKeyboardSelected(context)
    }

    // Standard auto-refresh using side effect
    LaunchedEffect(Unit) {
        // Repeatedly check state while page is visible
        while (true) {
            refreshStates()
            kotlinx.coroutines.delay(1500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Immersive atmospheric header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF261012), Color(0xFF140D0E))
                    )
                )
                .border(1.dp, Color(0xFFFF1F2D).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.stickme_logo_1781048421456),
                    contentDescription = "Stick Me Logo",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, Color(0xFFFF1F2D), RoundedCornerShape(14.dp))
                )

                Text(
                    text = "STICKME KEYBOARD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Insert custom cutouts and PNG stickers directly in any chat application instantly!",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // Status Badges
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusBadge(
                        label = "Enabled",
                        isActive = isEnabled,
                        modifier = Modifier.testTag("status_badge_enabled")
                    )
                    StatusBadge(
                        label = "Active Selected",
                        isActive = isSelected,
                        modifier = Modifier.testTag("status_badge_selected")
                    )
                }
            }
        }

        // Action panel triggers
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222A), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🔑 ACTIVATE EXTENSION IN 2 STEPS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                // Step 1 Flow
                InteractiveStepRow(
                    stepNumber = "1",
                    title = "Enable Keyboard",
                    descr = "Permit 'StickMe' in Settings > Language & Input > On-screen keyboards.",
                    isCompleted = isEnabled,
                    buttonLabel = "OPEN SETTINGS ⚙️",
                    onAction = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                )

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Step 2 Flow
                InteractiveStepRow(
                    stepNumber = "2",
                    title = "Set as Active Input",
                    descr = "Press 'Select' to switch your current typing framework and default to StickMe.",
                    isCompleted = isSelected,
                    buttonLabel = "SWITCH CURRENT KEYBOARD 🔄",
                    onAction = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    }
                )
            }
        }

        // Live Scratchpad Test Arena
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF22222A), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131317)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬 TEST DRILL FIELD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(
                        onClick = refreshStates,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = "Tap on the message box below to bring up the keyboard. Verify StickMe stickers output seamlessly!",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                var testInputText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = testInputText,
                    onValueChange = { testInputText = it },
                    placeholder = {
                        Text(
                            text = "Tap to type and test copy-pasting stickers...",
                            color = Color.DarkGray,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_keyboard_test_pad"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0C0C0E),
                        unfocusedContainerColor = Color(0xFF0C0C0E),
                        focusedBorderColor = Color(0xFFFF1F2D).copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0xFF1E1E24)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { testInputText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }

        // Additional information card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1C1C22), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tips Help",
                    tint = Color(0xFFFF1F2D),
                    modifier = Modifier.size(18.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Why use custom keyboard?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Because Google AI Studio and major platforms limit simple paste sharing APIs, the custom 'StickMe' keyboard routes high-fidelity transparent PNG sticker assets directly via the standard Android InputConnection content insertion pipeline. This allows lossless integration directly into WhatsApp, Telegram, Reels, or Slack messages!",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) Color(0xFF112B15) else Color(0xFF2C1315))
            .border(
                width = 1.dp,
                color = if (isActive) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336))
            )
            Text(
                text = "${label.uppercase()}: ${if (isActive) "ACTIVE" else "PENDING"}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) Color(0xFF81C784) else Color(0xFFE57373)
            )
        }
    }
}

@Composable
fun InteractiveStepRow(
    stepNumber: String,
    title: String,
    descr: String,
    isCompleted: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step bullet index representation
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isCompleted) Color(0xFF1B3D23) else Color(0xFF2A2A34))
                .border(
                    width = 1.dp,
                    color = if (isCompleted) Color(0xFF4CAF50) else Color.DarkGray,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = stepNumber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) Color.LightGray else Color.White
            )
            Text(
                text = descr,
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Color(0xFF2A2A30) else Color(0xFFFF1F2D),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buttonLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// Utility checker logic
fun isKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val enabledList = imm?.enabledInputMethodList ?: return false
    return enabledList.any { it.packageName == context.packageName }
}

fun isKeyboardSelected(context: Context): Boolean {
    val currentIme = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return currentIme != null && currentIme.contains(context.packageName)
}
