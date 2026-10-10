package com.antigravity.deadframeremover.engine

import com.antigravity.deadframeremover.logging.AppLogManager
import com.antigravity.deadframeremover.logging.LogLevel
import java.nio.ByteBuffer

/**
 * Ajustes del detector de duplicados.
 *
 * - [precise] = false es el modo Rápido de siempre (no cambia nada).
 * - [precise] = true activa el modo Preciso: compara por zonas y también el color.
 * - [sensitivity] solo se usa en modo Preciso: 0 = baja, 1 = media, 2 = alta
 *   (a mayor sensibilidad, se detectan cambios más pequeños).
 */
data class DetectionSettings(
    val precise: Boolean = false,
    val sensitivity: Int = 1
)

object NativeComparator {
    var isLoaded = false
        private set

    init {
        try {
            System.loadLibrary("frame_comparator")
            isLoaded = true
            AppLogManager.log(LogLevel.INFO, "NativeComparator", "libframe_comparator.so loaded successfully.")
        } catch (t: Throwable) {
            isLoaded = false
            AppLogManager.log(LogLevel.ERROR, "NativeComparator", "Failed to load libframe_comparator.so: ${t.message}")
        }
    }

    external fun comparePackedWithYPlane(
        packedPrevBuffer: ByteBuffer,
        currYBuffer: ByteBuffer,
        currYOffset: Int,
        currYRowStride: Int,
        currYPixelStride: Int,
        width: Int,
        height: Int
    ): Double

    external fun packYPlane(
        currYBuffer: ByteBuffer,
        currYOffset: Int,
        currYRowStride: Int,
        currYPixelStride: Int,
        width: Int,
        height: Int,
        packedDstBuffer: ByteBuffer
    ): Int

    /**
     * Modo Preciso. [prevNv12Buffer] debe contener el último fotograma conservado en NV12
     * (se rellena con [normalizeYUV420ToNV12], dstOffset = 0, y necesita width * height * 3 / 2 bytes).
     * Devuelve una puntuación que se compara con el umbral: puntuación <= umbral => duplicado.
     * Si [earlyExit] > 0 y ya se superó, devuelve antes un valor mínimo (suficiente para decidir).
     */
    external fun comparePreciseNV12(
        prevNv12Buffer: ByteBuffer,
        yBuffer: ByteBuffer, yOffset: Int, yRowStride: Int, yPixelStride: Int,
        uBuffer: ByteBuffer, uOffset: Int, uRowStride: Int, uPixelStride: Int,
        vBuffer: ByteBuffer, vOffset: Int, vRowStride: Int, vPixelStride: Int,
        width: Int, height: Int,
        sensitivity: Int,
        earlyExit: Double
    ): Double

    external fun compareYUVPlanes(
        bufferPrev: ByteBuffer,
        prevOffset: Int,
        bufferCurr: ByteBuffer,
        currOffset: Int,
        width: Int,
        height: Int,
        yRowStride: Int,
        yPixelStride: Int,
        threshold: Double
    ): Double

    external fun normalizeYUV420ToNV12(
        yBuffer: ByteBuffer, yOffset: Int, yRowStride: Int, yPixelStride: Int,
        uBuffer: ByteBuffer, uOffset: Int, uRowStride: Int, uPixelStride: Int,
        vBuffer: ByteBuffer, vOffset: Int, vRowStride: Int, vPixelStride: Int,
        dstBuffer: ByteBuffer, dstOffset: Int,
        width: Int, height: Int
    ): Int

    external fun yuvToRgbBitmap(
        yBuffer: ByteBuffer, yOffset: Int, yRowStride: Int, yPixelStride: Int,
        uBuffer: ByteBuffer, uOffset: Int, uRowStride: Int, uPixelStride: Int,
        vBuffer: ByteBuffer, vOffset: Int, vRowStride: Int, vPixelStride: Int,
        srcWidth: Int, srcHeight: Int,
        dstBitmap: android.graphics.Bitmap
    ): Int
}
