package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StickerGalleryPlaceholder(
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Elegant floating animation for the graphics to add visual polish
    val infiniteTransition = rememberInfiniteTransition(label = "placeholder_animation")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_graphic"
    )

    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_circle"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
        border = BorderStroke(1.dp, Color(0xFF1E1E24))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant placeholder graphic block
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .offset(y = floatAnim.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(pulseAnim)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Draw a subtle background cosmic radial glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF1F2D).copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(canvasWidth / 2, canvasHeight / 2),
                            radius = canvasWidth * 0.7f
                        )
                    )

                    // Draw dashed sticker outline contour
                    drawCircle(
                        color = Color(0xFFFF1F2D).copy(alpha = 0.4f),
                        radius = canvasWidth * 0.4f,
                        center = Offset(canvasWidth / 2, canvasHeight / 2),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )
                }

                // Layered Overlapping Icons representation (Sticker + Scissors + Camera)
                Box(contentAlignment = Alignment.Center) {
                    // Central Smiley Sticker icon (Big)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16161A))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🦄",
                            fontSize = 32.sp
                        )
                    }

                    // Bottom-Right mini camera icon badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E24))
                            .border(1.5.dp, Color.Gray, CircleShape)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = (-8).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📸",
                            fontSize = 12.sp
                        )
                    }

                    // Top-Left mini scissor cutout badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF1F2D))
                            .border(1.5.dp, Color.White, CircleShape)
                            .align(Alignment.TopStart)
                            .offset(x = 8.dp, y = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✂️",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Descriptive placeholder display-text block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "YOUR STICKER BOOK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "NO STICKERS RETRIEVED YET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF1F2D),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Transform cameras, snaps, photos, and digital art into premium physical-style outline sticker cutouts! Keep them stored locally in IndexedDB.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Divider(color = Color(0xFF1E1E24), thickness = 1.dp)

            // Step instructions (Scannable quick list)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepRow(
                    index = "1",
                    title = "Take/Pick a Photo",
                    body = "Snap a photo directly with your camera or import any image file from storage."
                )
                StepRow(
                    index = "2",
                    title = "Outline & Customize",
                    body = "Add high-contrast border outlines, dynamic shadows, and captions to form your perfect sticker."
                )
                StepRow(
                    index = "3",
                    title = "Save to IndexedDB",
                    body = "Commit your creation into transaction-safe browser-grade SQLite storage instantly."
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Button
            Button(
                onClick = onNavigateToStudio,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF2535), Color(0xFFB00610))
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("gallery_placeholder_to_studio_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Plus",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CREATE YOUR FIRST STICKER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun StepRow(
    index: String,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C33)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 14.sp
            )
        }
    }
}
