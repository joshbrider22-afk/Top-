package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraCaptureScreen(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera utilities
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Use cases
    val preview = remember { 
        Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build() 
    }
    val imageCapture = remember(lensFacing, flashMode) {
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
    }

    val cameraSelector = remember(lensFacing) {
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission required for sticker snap!", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("in_app_camera_screen")
    ) {
        if (!hasCameraPermission) {
            // High-fidelity rationale screen for permissions
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF1F2D).copy(alpha = 0.15f))
                        .border(1.5.dp, Color(0xFFFF1F2D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "CAMERA PERMISSION REQUIRED",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "We need direct camera access to let you capture stickers of your pets, drawings, toys, or daily objects in real-time, then magically outline them into cutouts!",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF1F2D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                ) {
                    Text("GRANT ACCESS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        } else {
            // Active Camera Viewport
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProvider.unbindAll()

                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraCaptureScreen", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // Sticker alignment overlay HUD
            StickerOverlayHUD()

            // Safe Area Controls Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Close trigger
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Camera",
                            tint = Color.White
                        )
                    }

                    // Dynamic Flash selector badge
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val modes = listOf(
                            Pair(ImageCapture.FLASH_MODE_OFF, "OFF 💤"),
                            Pair(ImageCapture.FLASH_MODE_ON, "ON ⚡"),
                            Pair(ImageCapture.FLASH_MODE_AUTO, "AUTO 💡")
                        )
                        modes.forEach { (mode, label) ->
                            val isSelected = flashMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFFFF1F2D) else Color.Transparent)
                                    .clickable { flashMode = mode }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.LightGray
                                )
                            }
                        }
                    }
                }

                // Middle spacing
                Spacer(modifier = Modifier.weight(1f))

                // Bottom Dashboard Panel Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lens Switcher (Front/Back)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C21))
                            .border(1.dp, Color.DarkGray, CircleShape)
                            .clickable {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔄", fontSize = 18.sp)
                    }

                    // Shutter Button Capture (Pulsating active state)
                    val scale by animateFloatAsState(
                        targetValue = if (isCapturing) 0.85f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "shutter_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .border(4.dp, Color.White, CircleShape)
                            .clickable(enabled = !isCapturing) {
                                isCapturing = true
                                val mainExecutor = ContextCompat.getMainExecutor(context)
                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val rawBitmap = image.toBitmap()
                                            image.close()

                                            if (rawBitmap != null) {
                                                // Mirror image if captured via front camera
                                                val processedBitmap = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                                    val matrix = Matrix().apply { postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f) }
                                                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                                } else {
                                                    rawBitmap
                                                }

                                                mainExecutor.execute {
                                                    onPhotoCaptured(processedBitmap)
                                                    isCapturing = false
                                                    onDismiss()
                                                }
                                            } else {
                                                mainExecutor.execute {
                                                    Toast.makeText(context, "Failed to decode photo", Toast.LENGTH_SHORT).show()
                                                    isCapturing = false
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("CameraCaptureScreen", "Photo capture failed", exception)
                                            mainExecutor.execute {
                                                Toast.makeText(context, "Capture error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                isCapturing = false
                                            }
                                        }
                                    }
                                )
                            }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isCapturing) Color(0xFFFF1F2D) else Color.White)
                        )
                    }

                    // Help/Info dialog button
                    var showInfoDialog by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C21))
                            .border(1.dp, Color.DarkGray, CircleShape)
                            .clickable { showInfoDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tips",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showInfoDialog) {
                        AlertDialog(
                            onDismissRequest = { showInfoDialog = false },
                            icon = { Text("💡", fontSize = 32.sp) },
                            title = { Text("PRO PHOTO TIPS", fontWeight = FontWeight.Black, fontSize = 16.sp) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("• Light is Key: Ensure the object is well lit. Use the flash if indoors.", fontSize = 12.sp, color = Color.LightGray)
                                    Text("• Solid Background: High contrast helps the background cutter work flawlessly.", fontSize = 12.sp, color = Color.LightGray)
                                    Text("• Frame It: Align your target object in the center circle guide to ensure maximum focus details.", fontSize = 12.sp, color = Color.LightGray)
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showInfoDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1F2D))
                                ) {
                                    Text("GOT IT")
                                }
                            },
                            containerColor = Color(0xFF16161A),
                            textContentColor = Color.LightGray,
                            titleContentColor = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StickerOverlayHUD() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val circleRadius = w * 0.35f
        val center = Offset(w / 2f, h / 2f)

        // 1. Semi-translucent mask around viewfinder
        // Draw the main dark boundary
        drawRect(
            color = Color.Black.copy(alpha = 0.3f)
        )

        // 2. Beautiful dash guidance circle for sticker placement
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = circleRadius,
            center = center,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        )

        // Guide tick marks
        val tickLength = 16.dp.toPx()
        val tickOffsetFromCircle = 4.dp.toPx()
        val r = circleRadius + tickOffsetFromCircle

        // Top tick
        drawLine(
            color = Color(0xFFFF1F2D),
            start = Offset(center.x, center.y - r),
            end = Offset(center.x, center.y - r - tickLength),
            strokeWidth = 3.dp.toPx()
        )
        // Bottom tick
        drawLine(
            color = Color(0xFFFF1F2D),
            start = Offset(center.x, center.y + r),
            end = Offset(center.x, center.y + r + tickLength),
            strokeWidth = 3.dp.toPx()
        )
        // Left tick
        drawLine(
            color = Color(0xFFFF1F2D),
            start = Offset(center.x - r, center.y),
            end = Offset(center.x - r - tickLength, center.y),
            strokeWidth = 3.dp.toPx()
        )
        // Right tick
        drawLine(
            color = Color(0xFFFF1F2D),
            start = Offset(center.x + r, center.y),
            end = Offset(center.x + r + tickLength, center.y),
            strokeWidth = 3.dp.toPx()
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset(y = (-110).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFF1F2D).copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "STICKER CAPTURE GUIDE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Center object in guidelines",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun ImageProxy.toBitmap(): Bitmap? {
    return try {
        // Fast buffer path directly on ImageProxy planes wrapper (much more reliable)
        val planes = this.planes
        if (planes.isEmpty()) return null
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        val finalBitmap = if (bitmap != null) {
            bitmap
        } else {
            // Fallback path to underlying media.Image if proxy buffer fails or is format-dependent
            val mediaImage = this.image
            if (mediaImage != null) {
                val mediaBuffer = mediaImage.planes[0].buffer
                mediaBuffer.rewind()
                val mediaBytes = ByteArray(mediaBuffer.remaining())
                mediaBuffer.get(mediaBytes)
                BitmapFactory.decodeByteArray(mediaBytes, 0, mediaBytes.size)
            } else {
                null
            }
        }

        if (finalBitmap != null) {
            val rotation = this.imageInfo.rotationDegrees
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(finalBitmap, 0, 0, finalBitmap.width, finalBitmap.height, matrix, true)
            } else {
                finalBitmap
            }
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
