@file:OptIn(ExperimentalMaterial3Api::class)

package com.antigravity.deadframeremover.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigravity.deadframeremover.engine.FrameItem
import com.antigravity.deadframeremover.engine.ProcessingProgress
import com.antigravity.deadframeremover.logging.AppLogManager
import com.antigravity.deadframeremover.logging.LogEntry
import com.antigravity.deadframeremover.logging.LogLevel
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Identidad visual. Cambia estos dos textos si quieres otro nombre dentro de la app.
// ---------------------------------------------------------------------------
private const val MARCA = "Depura"
private const val LEMA = "FOTOGRAMAS LIMPIOS"

// Rango del umbral (el mismo de siempre: 0.1 a 25.0, en pasos de 0.1)
private const val MIN_T = 0.1f
private const val MAX_T = 25f

private object Pal {
    val bgTop = Color(0xFF1B1912)
    val bg = Color(0xFF0F1013)
    val card = Color(0xFF181A1F)
    val cardHi = Color(0xFF21242B)
    val line = Color(0xFF2D3038)
    val text = Color(0xFFF3EFE7)
    val dim = Color(0xFFA9A59B)
    val accent = Color(0xFFE6C47E)
    val onAccent = Color(0xFF1B1507)
    val keep = Color(0xFF5BD3A2)
    val drop = Color(0xFFFF6F6F)
    val onDrop = Color(0xFF2A0B0B)
}

// ===========================================================================
// Pantalla principal (la firma de siempre + 4 parámetros opcionales del modo Preciso)
// ===========================================================================
@Composable
fun MainScreen(
    selectedUri: Uri?,
    selectedFileName: String?,
    mseThreshold: Float,
    isProcessing: Boolean,
    isAnalyzingFrames: Boolean,
    frames: List<FrameItem>,
    progress: ProcessingProgress,
    exportedFile: File?,
    savedCrashLog: String?,
    logs: List<LogEntry>,
    onSelectVideo: (Uri) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onAnalyzeFrames: () -> Unit,
    onToggleFrameSelection: (Int) -> Unit,
    onSelectAllGoodFrames: () -> Unit,
    onSelectAllFrames: () -> Unit,
    onInvertSelection: () -> Unit,
    onStartExport: (useSelection: Boolean) -> Unit,
    onCancelExport: () -> Unit,
    onOpenExportedVideo: (File) -> Unit,
    onClearCrashLog: () -> Unit,
    onClearLogs: () -> Unit,
    preciseMode: Boolean = false,
    detailSensitivity: Int = 1,
    onPreciseModeChange: (Boolean) -> Unit = {},
    onDetailSensitivityChange: (Int) -> Unit = {}
) {
    ScreenTheme {
        MainScreenContent(
            selectedUri = selectedUri,
            selectedFileName = selectedFileName,
            mseThreshold = mseThreshold,
            isProcessing = isProcessing,
            isAnalyzingFrames = isAnalyzingFrames,
            frames = frames,
            progress = progress,
            exportedFile = exportedFile,
            savedCrashLog = savedCrashLog,
            logs = logs,
            onSelectVideo = onSelectVideo,
            onThresholdChange = onThresholdChange,
            onAnalyzeFrames = onAnalyzeFrames,
            onToggleFrameSelection = onToggleFrameSelection,
            onSelectAllGoodFrames = onSelectAllGoodFrames,
            onSelectAllFrames = onSelectAllFrames,
            onInvertSelection = onInvertSelection,
            onStartExport = onStartExport,
            onCancelExport = onCancelExport,
            onOpenExportedVideo = onOpenExportedVideo,
            onClearCrashLog = onClearCrashLog,
            onClearLogs = onClearLogs,
            preciseMode = preciseMode,
            detailSensitivity = detailSensitivity,
            onPreciseModeChange = onPreciseModeChange,
            onDetailSensitivityChange = onDetailSensitivityChange
        )
    }
}

@Composable
private fun MainScreenContent(
    selectedUri: Uri?,
    selectedFileName: String?,
    mseThreshold: Float,
    isProcessing: Boolean,
    isAnalyzingFrames: Boolean,
    frames: List<FrameItem>,
    progress: ProcessingProgress,
    exportedFile: File?,
    savedCrashLog: String?,
    logs: List<LogEntry>,
    onSelectVideo: (Uri) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onAnalyzeFrames: () -> Unit,
    onToggleFrameSelection: (Int) -> Unit,
    onSelectAllGoodFrames: () -> Unit,
    onSelectAllFrames: () -> Unit,
    onInvertSelection: () -> Unit,
    onStartExport: (useSelection: Boolean) -> Unit,
    onCancelExport: () -> Unit,
    onOpenExportedVideo: (File) -> Unit,
    onClearCrashLog: () -> Unit,
    onClearLogs: () -> Unit,
    preciseMode: Boolean,
    detailSensitivity: Int,
    onPreciseModeChange: (Boolean) -> Unit,
    onDetailSensitivityChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showLogDialog by remember { mutableStateOf(false) }
    var selectedLogTab by remember { mutableIntStateOf(if (savedCrashLog != null) 1 else 0) }
    var showHelp by remember { mutableStateOf(false) }
    val busy = isProcessing || isAnalyzingFrames

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectVideo(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Pal.bgTop, Pal.bg)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Pal.text,
            bottomBar = {
                ActionDock(
                    isProcessing = isProcessing,
                    canExport = selectedUri != null && !isAnalyzingFrames,
                    hasFrames = frames.isNotEmpty(),
                    onStartExport = onStartExport,
                    onCancelExport = onCancelExport
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                AppHeader(
                    hasCrash = savedCrashLog != null,
                    onOpenLogs = {
                        if (savedCrashLog != null) selectedLogTab = 1
                        showLogDialog = true
                    }
                )

                if (savedCrashLog != null) {
                    CrashBanner(
                        onView = {
                            selectedLogTab = 1
                            showLogDialog = true
                        },
                        onDismiss = onClearCrashLog
                    )
                }

                VideoSection(
                    hasVideo = selectedUri != null,
                    fileName = selectedFileName,
                    busy = busy,
                    onPick = { videoPickerLauncher.launch("video/mp4") }
                )

                ThresholdSection(
                    value = mseThreshold,
                    busy = busy,
                    showHelp = showHelp,
                    onToggleHelp = { showHelp = !showHelp },
                    onChange = onThresholdChange,
                    preciseMode = preciseMode,
                    detailSensitivity = detailSensitivity,
                    onPreciseModeChange = onPreciseModeChange,
                    onDetailSensitivityChange = onDetailSensitivityChange
                )

                FramesSection(
                    hasVideo = selectedUri != null,
                    frames = frames,
                    isAnalyzing = isAnalyzingFrames,
                    busy = busy,
                    onAnalyze = onAnalyzeFrames,
                    onToggle = onToggleFrameSelection,
                    onSelectGood = onSelectAllGoodFrames,
                    onSelectAll = onSelectAllFrames,
                    onInvert = onInvertSelection
                )

                if (isProcessing || progress.progress > 0f || progress.totalScanned > 0) {
                    ProgressSection(progress = progress, isProcessing = isProcessing)
                }

                if (progress.errorMessage != null) {
                    ErrorNotice(message = progress.errorMessage)
                }

                if (progress.isCompleted && exportedFile != null && exportedFile.exists()) {
                    DoneCard(file = exportedFile, onOpen = { onOpenExportedVideo(exportedFile) })
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    // Diálogo de registros y fallos
    if (showLogDialog) {
        LogcatAndCrashDialog(
            logs = logs,
            savedCrashLog = savedCrashLog,
            selectedTab = selectedLogTab,
            onTabSelected = { selectedLogTab = it },
            onDismiss = { showLogDialog = false },
            onCopyLogs = {
                val success = AppLogManager.copyEntireLogToClipboard(context)
                if (success) {
                    Toast.makeText(context, "¡Registro completo copiado al portapapeles!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se pudo copiar el registro.", Toast.LENGTH_SHORT).show()
                }
            },
            onClearLogs = onClearLogs,
            onClearCrashLog = onClearCrashLog
        )
    }
}

// ===========================================================================
// Tema
// ===========================================================================
@Composable
private fun ScreenTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Pal.accent,
        onPrimary = Pal.onAccent,
        secondary = Pal.accent,
        onSecondary = Pal.onAccent,
        background = Pal.bg,
        onBackground = Pal.text,
        surface = Pal.card,
        onSurface = Pal.text,
        surfaceVariant = Pal.cardHi,
        onSurfaceVariant = Pal.dim,
        outline = Pal.line,
        outlineVariant = Pal.line,
        error = Pal.drop,
        onError = Pal.onDrop
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

// ===========================================================================
// Piezas de diseño reutilizables
// ===========================================================================
@Composable
private fun AppHeader(hasCrash: Boolean, onOpenLogs: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = MARCA,
                    color = Pal.text,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .padding(bottom = 11.dp)
                        .size(7.dp)
                        .background(Pal.accent, CircleShape)
                )
            }
            Text(
                text = LEMA,
                color = Pal.dim,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
        }
        IconButton(onClick = onOpenLogs) {
            BadgedBox(
                badge = {
                    if (hasCrash) {
                        Badge(containerColor = Pal.drop) {
                            Text("!")
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Abrir registros y fallos",
                    tint = Pal.dim
                )
            }
        }
    }
}

@Composable
private fun StepLabel(number: String, title: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = number,
            color = Pal.accent,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = Pal.dim,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Pal.cardHi, Pal.card)))
            .border(1.dp, Pal.line, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
private fun RoundStepButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Pal.accent.copy(alpha = if (enabled) 0.16f else 0.06f))
            .border(1.dp, Pal.accent.copy(alpha = if (enabled) 0.45f else 0.15f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) Pal.accent else Pal.dim
        )
    }
}

@Composable
private fun PresetChip(
    modifier: Modifier,
    label: String,
    value: Float,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Pal.accent else Pal.line.copy(alpha = 0.55f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format(Locale.US, "%.1f", value),
            color = if (selected) Pal.onAccent else Pal.text,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = if (selected) Pal.onAccent else Pal.dim,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OutlineChip(
    modifier: Modifier,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .border(1.dp, Pal.line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Pal.text else Pal.dim,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SegmentedChoice(
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.line.copy(alpha = 0.45f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Pal.accent else Color.Transparent)
                    .clickable(enabled = enabled, onClick = { onSelect(index) })
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = when {
                        selected -> Pal.onAccent
                        enabled -> Pal.text
                        else -> Pal.dim
                    },
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = Pal.dim, fontSize = 12.sp)
    }
}

@Composable
private fun StatRow(label: String, value: Int, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accent, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = Pal.dim,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toString(),
            color = Pal.text,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

// ===========================================================================
// Aviso de fallo anterior
// ===========================================================================
@Composable
private fun CrashBanner(onView: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.drop.copy(alpha = 0.12f))
            .border(1.dp, Pal.drop.copy(alpha = 0.45f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Pal.drop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "La app se cerró de forma inesperada",
                color = Pal.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
        Text(
            text = "La última vez ocurrió un fallo grave y quedó registrado. Puedes revisar el detalle técnico y copiarlo.",
            color = Pal.dim,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onView,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.drop,
                    contentColor = Pal.onDrop
                )
            ) {
                Text("Ver informe")
            }
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Pal.line)
            ) {
                Text("Descartar", color = Pal.text)
            }
        }
    }
}

// ===========================================================================
// 01 · Video
// ===========================================================================
@Composable
private fun VideoSection(
    hasVideo: Boolean,
    fileName: String?,
    busy: Boolean,
    onPick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StepLabel(number = "01", title = "VIDEO")
        if (!hasVideo) {
            DropZone(enabled = !busy, onClick = onPick)
        } else {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Pal.accent.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Pal.accent
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName ?: "Video seleccionado",
                            color = Pal.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "MP4 listo para analizar",
                            color = Pal.dim,
                            fontSize = 12.sp
                        )
                    }
                }
                OutlinedButton(
                    onClick = onPick,
                    enabled = !busy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Pal.line),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = Pal.accent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Cambiar video", color = Pal.text)
                }
            }
        }
    }
}

@Composable
private fun DropZone(enabled: Boolean, onClick: () -> Unit) {
    val dash = Pal.accent.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Pal.card)
            .clickable(enabled = enabled, onClick = onClick)
            .drawBehind {
                val w = 1.5.dp.toPx()
                drawRoundRect(
                    color = dash,
                    topLeft = Offset(w / 2f, w / 2f),
                    size = Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(26.dp.toPx() - w / 2f),
                    style = Stroke(
                        width = w,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                    )
                )
            }
            .padding(vertical = 34.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Pal.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Pal.onAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Elige un video MP4",
                color = Pal.text,
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Toca aquí para buscarlo en tu teléfono",
                color = Pal.dim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ===========================================================================
// 02 · Sensibilidad (umbral)
// ===========================================================================
@Composable
private fun ThresholdSection(
    value: Float,
    busy: Boolean,
    showHelp: Boolean,
    onToggleHelp: () -> Unit,
    onChange: (Float) -> Unit,
    preciseMode: Boolean,
    detailSensitivity: Int,
    onPreciseModeChange: (Boolean) -> Unit,
    onDetailSensitivityChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StepLabel(number = "02", title = "SENSIBILIDAD")
        Panel {
            Text(
                text = "Umbral de duplicados",
                color = Pal.text,
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp
            )

            // Modo de detección: Rápido (el de siempre) o Preciso (por zonas y con color)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedChoice(
                    options = listOf("Rápido", "Preciso"),
                    selectedIndex = if (preciseMode) 1 else 0,
                    enabled = !busy,
                    onSelect = { index -> onPreciseModeChange(index == 1) }
                )
                Text(
                    text = if (preciseMode) {
                        "Revisa la imagen por zonas y también el color. Detecta movimientos pequeños, como una boca o un parpadeo. Puede conservar más fotogramas."
                    } else {
                        "Compara el brillo de toda la imagen. Es el modo de siempre y el más veloz."
                    },
                    color = Pal.dim,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            if (preciseMode) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sensibilidad a detalles pequeños",
                        color = Pal.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    SegmentedChoice(
                        options = listOf("Baja", "Media", "Alta"),
                        selectedIndex = detailSensitivity.coerceIn(0, 2),
                        enabled = !busy,
                        onSelect = { index -> onDetailSensitivityChange(index) }
                    )
                    Text(
                        text = when (detailSensitivity) {
                            0 -> "Ignora los cambios muy pequeños: se descartan más duplicados."
                            2 -> "Detecta hasta el cambio más mínimo: se conservan más fotogramas, también los de ruido de compresión. Si quedan duplicados, sube el umbral."
                            else -> "Equilibrada: detecta bocas, parpadeos y destellos. Es el punto de partida recomendado."
                        },
                        color = Pal.dim,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            // Valor grande con botones − / + para ajustar de 0.1 en 0.1
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoundStepButton(
                    icon = Icons.Default.Remove,
                    description = "Bajar el umbral",
                    enabled = !busy && value > MIN_T + 0.05f,
                    onClick = { onChange(snapThreshold(value - 0.1f)) }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%.1f", value),
                        color = Pal.accent,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp
                    )
                    Text(
                        text = "MSE",
                        color = Pal.dim,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                }
                RoundStepButton(
                    icon = Icons.Default.Add,
                    description = "Subir el umbral",
                    enabled = !busy && value < MAX_T - 0.05f,
                    onClick = { onChange(snapThreshold(value + 0.1f)) }
                )
            }

            // Barra con escala logarítmica: la mitad izquierda cubre 0.1 a 1.6,
            // donde se necesita más precisión, y no dibuja 249 marcas.
            Column {
                Slider(
                    value = thresholdToPos(value),
                    onValueChange = { p -> onChange(snapThreshold(posToThreshold(p))) },
                    valueRange = 0f..1f,
                    enabled = !busy,
                    colors = SliderDefaults.colors(
                        thumbColor = Pal.accent,
                        activeTrackColor = Pal.accent,
                        inactiveTrackColor = Pal.line,
                        disabledThumbColor = Pal.dim,
                        disabledActiveTrackColor = Pal.dim,
                        disabledInactiveTrackColor = Pal.line
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Solo idénticos", color = Pal.dim, fontSize = 11.sp)
                    Text(text = "Muy agresivo", color = Pal.dim, fontSize = 11.sp)
                }
            }

            // Atajos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    modifier = Modifier.weight(1f),
                    label = "Estricto",
                    value = 0.5f,
                    selected = abs(value - 0.5f) < 0.05f,
                    enabled = !busy,
                    onClick = { onChange(0.5f) }
                )
                PresetChip(
                    modifier = Modifier.weight(1f),
                    label = "Pantalla",
                    value = 1.5f,
                    selected = abs(value - 1.5f) < 0.05f,
                    enabled = !busy,
                    onClick = { onChange(1.5f) }
                )
                PresetChip(
                    modifier = Modifier.weight(1f),
                    label = "Cámara",
                    value = 3.0f,
                    selected = abs(value - 3.0f) < 0.05f,
                    enabled = !busy,
                    onClick = { onChange(3.0f) }
                )
            }

            TextButton(
                onClick = onToggleHelp,
                colors = ButtonDefaults.textButtonColors(contentColor = Pal.accent)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (showHelp) "Ocultar explicación" else "¿Cómo funciona?")
            }
            if (showHelp) {
                Text(
                    text = "Los fotogramas con error cuadrático medio (MSE) menor o igual al umbral se descartan como duplicados o congelados. En grabaciones de pantalla y juegos, entre 0.5 y 2.0 elimina los congelamientos exactos. En videos de cámara, entre 2.0 y 5.0 detecta duplicados sutiles causados por la compresión. En modo Preciso el mismo umbral se aplica por zonas: si cambias el modo o la sensibilidad, pulsa «Analizar de nuevo» para ver el resultado en las miniaturas.",
                    color = Pal.dim,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun snapThreshold(v: Float): Float =
    ((v * 10f).roundToInt() / 10f).coerceIn(MIN_T, MAX_T)

private fun thresholdToPos(v: Float): Float =
    (ln(v.coerceIn(MIN_T, MAX_T) / MIN_T) / ln(MAX_T / MIN_T)).coerceIn(0f, 1f)

private fun posToThreshold(p: Float): Float =
    MIN_T * (MAX_T / MIN_T).pow(p.coerceIn(0f, 1f))

// ===========================================================================
// 03 · Fotogramas
// ===========================================================================
@Composable
private fun FramesSection(
    hasVideo: Boolean,
    frames: List<FrameItem>,
    isAnalyzing: Boolean,
    busy: Boolean,
    onAnalyze: () -> Unit,
    onToggle: (Int) -> Unit,
    onSelectGood: () -> Unit,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StepLabel(number = "03", title = "FOTOGRAMAS")
        Panel {
            if (frames.isEmpty() && !isAnalyzing) {
                Text(
                    text = "Analiza el video para ver cada fotograma, detectar los congelados (en rojo) y elegir cuáles exportar.",
                    color = Pal.dim,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Button(
                onClick = onAnalyze,
                enabled = hasVideo && !busy,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.accent,
                    contentColor = Pal.onAccent,
                    disabledContainerColor = Pal.line,
                    disabledContentColor = Pal.dim
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Pal.onAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Analizando…", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (frames.isEmpty()) "Analizar fotogramas" else "Analizar de nuevo",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isAnalyzing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp)),
                    color = Pal.accent,
                    trackColor = Pal.line
                )
            }

            if (frames.isNotEmpty()) {
                val goodCount = frames.count { !it.isDead }
                val deadCount = frames.count { it.isDead }
                val selectedCount = frames.count { it.isSelected }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = selectedCount.toString(),
                            color = Pal.accent,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 44.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "de ${frames.size} fotogramas se conservan",
                            color = Pal.text,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 9.dp)
                        )
                    }
                    Text(
                        text = "$deadCount duplicados detectados · $goodCount buenos",
                        color = Pal.dim,
                        fontSize = 12.sp
                    )
                }

                FrameMap(frames = frames)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LegendDot(color = Pal.keep, label = "Se conservan")
                    LegendDot(color = Pal.drop, label = "Se eliminan")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlineChip(
                        modifier = Modifier.weight(1f),
                        label = "Buenos",
                        enabled = !busy,
                        onClick = onSelectGood
                    )
                    OutlineChip(
                        modifier = Modifier.weight(1f),
                        label = "Todos",
                        enabled = !busy,
                        onClick = onSelectAll
                    )
                    OutlineChip(
                        modifier = Modifier.weight(1f),
                        label = "Invertir",
                        enabled = !busy,
                        onClick = onInvert
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(frames, key = { it.index }) { frame ->
                        FrameTile(
                            frame = frame,
                            onToggle = { onToggle(frame.index) }
                        )
                    }
                }

                Text(
                    text = "Toca un fotograma para conservarlo o eliminarlo.",
                    color = Pal.dim,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Mapa de todo el video en una sola línea: un trazo por fotograma,
 * verde si se conserva y rojo si se elimina.
 */
@Composable
private fun FrameMap(frames: List<FrameItem>) {
    val keepColor = Pal.keep
    val dropColor = Pal.drop
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Pal.line)
    ) {
        val n = frames.size
        if (n > 0) {
            val w = size.width / n
            frames.forEachIndexed { i, f ->
                drawRect(
                    color = if (f.isSelected) keepColor else dropColor,
                    topLeft = Offset(i * w, 0f),
                    size = Size(w + 0.6f, size.height)
                )
            }
        }
    }
}

/**
 * Fotograma: miniatura con borde verde si se conserva o rojo si se elimina.
 */
@Composable
private fun FrameTile(frame: FrameItem, onToggle: () -> Unit) {
    val keep = frame.isSelected
    val accent = if (keep) Pal.keep else Pal.drop
    val shape = RoundedCornerShape(18.dp)
    val bmp = frame.bitmap

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(166.dp)
            .clip(shape)
            .background(Pal.bg)
            .border(if (keep) 2.dp else 1.5.dp, accent, shape)
            .clickable(onClick = onToggle)
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (keep) 1f else 0.42f)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin vista previa",
                    color = Pal.dim,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Degradado inferior para que el texto se lea sobre cualquier imagen
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(78.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        // Número del fotograma
        Text(
            text = "#${frame.index}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )

        // Marca de estado
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (keep) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (keep) "Se conserva" else "Se elimina",
                tint = Pal.onDrop,
                modifier = Modifier.size(16.dp)
            )
        }

        // Pie: tiempo, MSE y tipo detectado
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = frame.formattedTime,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (frame.index == 0) "Base" else String.format(Locale.US, "MSE %.1f", frame.mse),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = if (frame.isDead) "Duplicado" else "Movimiento",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ===========================================================================
// 04 · Progreso
// ===========================================================================
@Composable
private fun ProgressSection(progress: ProcessingProgress, isProcessing: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StepLabel(number = "04", title = "PROGRESO")
        Panel {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = (progress.progress * 100f).toInt().coerceIn(0, 100).toString(),
                    color = Pal.accent,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                )
                Text(
                    text = "%",
                    color = Pal.accent,
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 9.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isProcessing) "Procesando…" else if (progress.isCompleted) "Completado" else "En pausa",
                    color = Pal.dim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            LinearProgressIndicator(
                progress = { progress.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Pal.accent,
                trackColor = Pal.line
            )
            StatRow(label = "Fotogramas analizados", value = progress.totalScanned, accent = Pal.accent)
            StatRow(label = "Eliminados", value = progress.droppedFrames, accent = Pal.drop)
            StatRow(label = "Conservados", value = progress.preservedFrames, accent = Pal.keep)
        }
    }
}

@Composable
private fun ErrorNotice(message: String?) {
    val cancelled = message != null &&
        (message.startsWith("Processing cancelled") || message.startsWith("Proceso cancelado"))
    val shape = RoundedCornerShape(22.dp)
    val tone = if (cancelled) Pal.accent else Pal.drop
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.45f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = tone
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (cancelled) "Proceso cancelado" else "No se pudo completar",
                color = Pal.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            if (!cancelled) {
                Text(
                    text = translateError(message),
                    color = Pal.dim,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun DoneCard(file: File, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val bytes = file.length()
    val sizeText = if (bytes >= 1_048_576L) {
        String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    } else {
        "${bytes / 1024} KB"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Pal.keep.copy(alpha = 0.22f), Pal.card)))
            .border(1.dp, Pal.keep.copy(alpha = 0.55f), shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Pal.keep, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Pal.onAccent
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Video listo",
                color = Pal.text,
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp
            )
        }
        Text(
            text = "${file.name} · $sizeText",
            color = Pal.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Guardado en Descargas: ${file.absolutePath}",
            color = Pal.dim,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Button(
            onClick = onOpen,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Pal.keep,
                contentColor = Pal.onAccent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Reproducir video", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ===========================================================================
// Barra inferior fija con las acciones de exportación
// ===========================================================================
@Composable
private fun ActionDock(
    isProcessing: Boolean,
    canExport: Boolean,
    hasFrames: Boolean,
    onStartExport: (Boolean) -> Unit,
    onCancelExport: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.card)
            .border(1.dp, Pal.line, shape)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isProcessing) {
            OutlinedButton(
                onClick = onCancelExport,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Pal.drop),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Pal.drop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancelar proceso",
                    color = Pal.drop,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Button(
                onClick = { onStartExport(hasFrames) },
                enabled = canExport,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.accent,
                    contentColor = Pal.onAccent,
                    disabledContainerColor = Pal.line,
                    disabledContentColor = Pal.dim
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Icon(
                    imageVector = if (hasFrames) Icons.Default.AutoFixHigh else Icons.Default.Speed,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (hasFrames) "Exportar selección" else "Exportar video limpio",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
            if (hasFrames) {
                TextButton(
                    onClick = { onStartExport(false) },
                    enabled = canExport,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Pal.accent,
                        disabledContentColor = Pal.dim
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quitar duplicados automáticamente",
                        textAlign = TextAlign.Center
                    )
                }
            }
            Text(
                text = "Codificación H.264 por hardware (MediaCodec)",
                color = Pal.dim,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ===========================================================================
// Registros y fallos
// ===========================================================================
@Composable
private fun LogcatAndCrashDialog(
    logs: List<LogEntry>,
    savedCrashLog: String?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onCopyLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onClearCrashLog: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(28.dp),
            color = Pal.card,
            contentColor = Pal.text
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Título
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Pal.accent
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Diagnóstico",
                            color = Pal.text,
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Pal.text
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pestañas: registro en vivo e informe de fallos
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Pal.accent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text("Registro (${logs.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Informe de fallos")
                                if (savedCrashLog != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Pal.drop, CircleShape)
                                    )
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Texto del registro
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Pal.bg, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        if (logs.isEmpty()) {
                            Text(
                                text = "Aún no se han capturado mensajes de registro.",
                                color = Pal.dim,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                logs.forEach { entry ->
                                    val textColor = when (entry.level) {
                                        LogLevel.ERROR -> Color(0xFFEF5350)
                                        LogLevel.WARN -> Color(0xFFFFB74D)
                                        LogLevel.INFO -> Color(0xFF81C784)
                                        LogLevel.FFMPEG -> Color(0xFF4FC3F7)
                                        LogLevel.DEBUG -> Color(0xFFB0BEC5)
                                    }
                                    Text(
                                        text = "[${entry.timestamp}] [${entry.level.name}] [${entry.tag}]: ${entry.message}",
                                        color = textColor,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    } else {
                        if (savedCrashLog != null) {
                            Text(
                                text = savedCrashLog,
                                color = Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            Text(
                                text = "No hay informe de fallos. La aplicación no ha sufrido cierres inesperados.",
                                color = Pal.keep,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botones inferiores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCopyLogs,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Pal.accent,
                            contentColor = Pal.onAccent
                        )
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copiar registro completo",
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (selectedTab == 0) {
                        OutlinedButton(
                            onClick = onClearLogs,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Pal.line)
                        ) {
                            Text(text = "Borrar", color = Pal.text)
                        }
                    } else if (savedCrashLog != null) {
                        OutlinedButton(
                            onClick = onClearCrashLog,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Pal.drop)
                        ) {
                            Text(text = "Borrar informe", color = Pal.drop)
                        }
                    }
                }
            }
        }
    }
}

// Traduce los pocos mensajes de error que genera MainActivity en inglés
private fun translateError(message: String?): String = when (message) {
    "Processing cancelled by user." -> "Proceso cancelado por el usuario."
    "Export error" -> "Error de exportación"
    "Failed to create MediaStore entry in Downloads" -> "No se pudo crear el archivo en Descargas."
    else -> message ?: ""
}
