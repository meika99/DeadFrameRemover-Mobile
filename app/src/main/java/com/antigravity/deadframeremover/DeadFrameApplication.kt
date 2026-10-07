@file:OptIn(ExperimentalMaterial3Api::class)

package com.antigravity.deadframeremover.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
    onClearLogs: () -> Unit
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
            onClearLogs = onClearLogs
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
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showLogDialog by remember { mutableStateOf(false) }
    var selectedLogTab by remember { mutableIntStateOf(if (savedCrashLog != null) 1 else 0) }
    val busy = isProcessing || isAnalyzingFrames

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectVideo(uri)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "DeadFrameRemover",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    // Botón de registros y fallos
                    IconButton(onClick = {
                        if (savedCrashLog != null) selectedLogTab = 1
                        showLogDialog = true
                    }) {
                        BadgedBox(
                            badge = {
                                if (savedCrashLog != null) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("!")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Abrir registros y fallos"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Aviso de fallo anterior
            if (savedCrashLog != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Se detectó un cierre inesperado",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "La última vez, la app se cerró por un fallo grave y quedó registrado. Puedes revisar el detalle técnico y copiar el registro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    selectedLogTab = 1
                                    showLogDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Ver informe")
                            }
                            OutlinedButton(
                                onClick = onClearCrashLog,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Descartar")
                            }
                        }
                    }
                }
            }

            // 1. Video de entrada
            SectionCard(step = 1, title = "Video de entrada") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = if (selectedFileName != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = selectedFileName ?: "Aún no has elegido ningún video MP4.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedFileName != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Button(
                    onClick = { videoPickerLauncher.launch("video/mp4") },
                    enabled = !busy,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedUri == null) "Elegir video MP4" else "Cambiar video")
                }
            }

            // 2. Umbral de duplicados
            SectionCard(
                step = 2,
                title = "Umbral de duplicados (MSE)",
                trailing = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f", mseThreshold),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            ) {
                Text(
                    text = "Los fotogramas con error cuadrático medio (MSE) menor o igual al umbral se descartan como duplicados o congelados. En grabaciones de pantalla y juegos, entre 0.5 y 2.0 elimina los congelamientos exactos. En videos de cámara, entre 2.0 y 5.0 detecta duplicados sutiles causados por la compresión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Slider(
                        value = mseThreshold,
                        onValueChange = onThresholdChange,
                        valueRange = 0.1f..25.0f,
                        steps = 248,
                        enabled = !busy,
                        colors = SliderDefaults.colors(
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0.1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "25.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Visor y selector de fotogramas
            SectionCard(step = 3, title = "Visor y selector de fotogramas") {
                Text(
                    text = "Verde = se conserva · Rojo = se elimina. Toca un fotograma para cambiarlo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onAnalyzeFrames,
                    enabled = selectedUri != null && !busy,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isAnalyzingFrames) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analizando…")
                    } else {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analizar fotogramas")
                    }
                }

                if (frames.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSelectAllGoodFrames,
                            enabled = !busy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Elegir buenos")
                        }
                        OutlinedButton(
                            onClick = onInvertSelection,
                            enabled = !busy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Invertir")
                        }
                    }

                    val goodCount = frames.count { !it.isDead }
                    val deadCount = frames.count { it.isDead }
                    val selectedCount = frames.count { it.isSelected }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            value = frames.size.toString(),
                            label = "Total",
                            accent = MaterialTheme.colorScheme.onSurface
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            value = goodCount.toString(),
                            label = "Buenos",
                            accent = ScreenKeepColor
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            value = deadCount.toString(),
                            label = "Duplicados",
                            accent = ScreenDropColor
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            value = selectedCount.toString(),
                            label = "Elegidos",
                            accent = MaterialTheme.colorScheme.primary
                        )
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(216.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(frames, key = { it.index }) { frame ->
                            FrameCardItem(
                                frame = frame,
                                onToggle = { onToggleFrameSelection(frame.index) }
                            )
                        }
                    }
                } else if (!isAnalyzingFrames) {
                    Text(
                        text = "Toca «Analizar fotogramas» para inspeccionar los fotogramas del video, ver en rojo los congelados y elegir cuáles exportar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // 4. Progreso y métricas
            SectionCard(step = 4, title = "Progreso y métricas") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        modifier = Modifier.weight(1f),
                        value = progress.totalScanned.toString(),
                        label = "Analizados",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    StatTile(
                        modifier = Modifier.weight(1f),
                        value = progress.droppedFrames.toString(),
                        label = "Eliminados",
                        accent = ScreenDropColor
                    )
                    StatTile(
                        modifier = Modifier.weight(1f),
                        value = progress.preservedFrames.toString(),
                        label = "Conservados",
                        accent = ScreenKeepColor
                    )
                }

                if (isProcessing || progress.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Text(
                        text = String.format(Locale.US, "Progreso: %.1f%%", progress.progress * 100f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Aviso de error
            if (progress.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Error: ${translateError(progress.errorMessage)}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Exportación completada
            if (progress.isCompleted && exportedFile != null && exportedFile.exists()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = ScreenKeepColor.copy(alpha = 0.14f)),
                    border = BorderStroke(1.dp, ScreenKeepColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ScreenKeepColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "¡Video exportado!",
                                style = MaterialTheme.typography.titleMedium,
                                color = ScreenKeepColor
                            )
                        }
                        Text(
                            text = "Archivo: ${exportedFile.name} (${exportedFile.length() / 1024} KB)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Guardado en Descargas: ${exportedFile.absolutePath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { onOpenExportedVideo(exportedFile) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScreenKeepColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reproducir video exportado")
                        }
                    }
                }
            }

            // Botones de exportación
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isProcessing) {
                    OutlinedButton(
                        onClick = onCancelExport,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancelar proceso")
                    }
                } else {
                    if (frames.isNotEmpty()) {
                        Button(
                            onClick = { onStartExport(true) },
                            enabled = selectedUri != null && !isAnalyzingFrames,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar fotogramas elegidos")
                        }

                        FilledTonalButton(
                            onClick = { onStartExport(false) },
                            enabled = selectedUri != null && !isAnalyzingFrames,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Eliminar duplicados automáticamente")
                        }
                    } else {
                        Button(
                            onClick = { onStartExport(false) },
                            enabled = selectedUri != null && !isAnalyzingFrames,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar MP4 limpio")
                        }
                    }
                    Text(
                        text = "Codificación H.264 por hardware (MediaCodec)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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

/**
 * Tarjeta de sección numerada: círculo con el paso, título y contenido.
 */
@Composable
private fun SectionCard(
    step: Int,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                trailing?.invoke()
            }
            content()
        }
    }
}

/**
 * Mosaico de estadística: número grande con color de acento y etiqueta pequeña.
 */
@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tarjeta de fotograma:
 * borde verde si se conserva, rojo si se elimina; etiqueta con el tipo detectado.
 */
@Composable
private fun FrameCardItem(
    frame: FrameItem,
    onToggle: () -> Unit
) {
    val keep = frame.isSelected
    val accent = if (keep) ScreenKeepColor else ScreenDropColor
    val stateText = if (keep) "CONSERVAR" else "ELIMINAR"
    val detectTag = if (frame.isDead) "Duplicado" else "Movimiento"

    Card(
        onClick = onToggle,
        modifier = Modifier
            .width(164.dp)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        border = BorderStroke(if (keep) 2.dp else 1.5.dp, accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabecera: número de fotograma y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${frame.index}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = stateText,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Miniatura con etiqueta del tipo detectado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                if (frame.bitmap != null) {
                    Image(
                        bitmap = frame.bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin vista previa", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = detectTag,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Pie: tiempo, MSE y casilla
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = frame.formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (frame.index == 0) "Base" else String.format(Locale.US, "MSE: %.1f", frame.mse),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (frame.isDead) ScreenDropColor else ScreenKeepColor
                    )
                }

                Checkbox(
                    checked = frame.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ScreenKeepColor,
                        uncheckedColor = ScreenDropColor
                    )
                )
            }
        }
    }
}

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
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1B1D21)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Barra de título
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
                            tint = Color(0xFF8EB4FF)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Diagnóstico y fallos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pestañas: registro en vivo y informe de fallos
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF24272C),
                    contentColor = Color.White
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
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Área de texto del registro
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0F1114), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        if (logs.isEmpty()) {
                            Text(
                                text = "Aún no se han capturado mensajes de registro.",
                                color = Color.Gray,
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
                                color = Color(0xFF81C784),
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F5FD0))
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copiar registro completo", fontWeight = FontWeight.Bold)
                    }

                    if (selectedTab == 0) {
                        OutlinedButton(
                            onClick = onClearLogs,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Borrar")
                        }
                    } else if (savedCrashLog != null) {
                        OutlinedButton(
                            onClick = onClearCrashLog,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                        ) {
                            Text("Borrar informe")
                        }
                    }
                }
            }
        }
    }
}

// ---------- Tema (incluido aqui para no depender de otro archivo) ----------

private val ScreenKeepColor = Color(0xFF2E9E5B)
private val ScreenDropColor = Color(0xFFE0524D)

private val ScreenDarkColors = darkColorScheme(
    primary = Color(0xFF8EB4FF),
    onPrimary = Color(0xFF002E6E),
    primaryContainer = Color(0xFF1B4690),
    onPrimaryContainer = Color(0xFFD9E5FF),
    secondary = Color(0xFFB7C6EA),
    onSecondary = Color(0xFF223250),
    secondaryContainer = Color(0xFF384868),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFF7FD9C8),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005144),
    onTertiaryContainer = Color(0xFF9CF5E3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF2B2F38),
    onSurfaceVariant = Color(0xFFC3C6D0),
    outline = Color(0xFF8D909A),
    outlineVariant = Color(0xFF43474F),
    surfaceContainerLowest = Color(0xFF0E0E10),
    surfaceContainerLow = Color(0xFF1A1B1F),
    surfaceContainer = Color(0xFF1E2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A)
)

private val ScreenLightColors = lightColorScheme(
    primary = Color(0xFF2F5FD0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF565E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2C),
    tertiary = Color(0xFF006B5C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFA0F2E0),
    onTertiaryContainer = Color(0xFF00201B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFF6F7FB),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF0F6),
    surfaceContainerHigh = Color(0xFFE7E9F0),
    surfaceContainerHighest = Color(0xFFE1E3EA)
)

private val ScreenBaseTypography = Typography()

private val ScreenTypography = Typography(
    titleLarge = ScreenBaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = ScreenBaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = ScreenBaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
)

private val ScreenShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
private fun ScreenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ScreenDarkColors else ScreenLightColors,
        typography = ScreenTypography,
        shapes = ScreenShapes,
        content = content
    )
}

private fun translateError(message: String?): String = when (message) {
    "Processing cancelled by user." -> "Proceso cancelado por el usuario."
    "Export error" -> "Error de exportación"
    else -> message ?: ""
}
