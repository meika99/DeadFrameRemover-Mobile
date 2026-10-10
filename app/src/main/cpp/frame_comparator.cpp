#include <jni.h>
#include <android/bitmap.h>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <algorithm>
#include <vector>

// ---------------------------------------------------------------------------
// Modo "Preciso": comparación por bloques (luma + croma).
//
// A diferencia del modo Rápido (promedio global de un cuarto de los píxeles de
// brillo), este modo mira TODOS los píxeles, incluye el color y puntúa cada
// bloque por separado. Un movimiento pequeño (una boca, un parpadeo) cambia
// muy poco el promedio global, pero dentro de su bloque el cambio es grande.
//
//   puntuación = max( promedio global , bloque más cambiado / factor )
//
// El resultado se compara con el MISMO umbral del slider (puntuación <= umbral
// => fotograma duplicado), así que no hace falta cambiar nada más en Kotlin.
//
// "Sensibilidad a detalles pequeños" = tamaño de bloque + factor:
//   baja  : bloques de 32 px, factor 20 -> ignora cambios muy pequeños
//   media : bloques de 16 px, factor 16
//   alta  : bloques de  8 px, factor 10 -> detecta hasta el cambio más mínimo
// ---------------------------------------------------------------------------
namespace {

constexpr double kChromaWeight = 0.5;      // peso del color frente al brillo
constexpr int kMinBlockLumaSamples = 32;   // ignora bloques de borde diminutos

struct PreciseLevel {
    int block;
    double factor;
};

// Índice = sensibilidad: 0 baja, 1 media, 2 alta.
constexpr PreciseLevel kPreciseLevels[3] = {
    {32, 20.0},
    {16, 16.0},
    {8, 10.0},
};

// prev: NV12 empaquetado (plano Y de width*height seguido del plano UV
// intercalado con width bytes por fila de croma), tal como lo escribe
// normalizeYUV420ToNV12 con dstOffset = 0.
double ComparePreciseCore(
    const uint8_t* prev,
    const uint8_t* y_cur, int y_rs, int y_ps,
    const uint8_t* u_cur, int u_rs, int u_ps,
    const uint8_t* v_cur, int v_rs, int v_ps,
    int width, int height,
    int block, double block_factor, double early_exit
) {
    const int cw = width / 2;
    const int ch = height / 2;
    const int nbx = (width + block - 1) / block;
    const int nby = (height + block - 1) / block;
    const uint8_t* prev_uv = prev + static_cast<size_t>(width) * static_cast<size_t>(height);

    std::vector<uint32_t> luma_acc(static_cast<size_t>(nbx));
    std::vector<uint32_t> chroma_acc(static_cast<size_t>(nbx));

    uint64_t global_luma = 0;
    uint64_t global_chroma = 0;
    double max_block = 0.0;

    for (int by = 0; by < nby; ++by) {
        const int y0 = by * block;
        const int y1 = std::min(height, y0 + block);
        std::fill(luma_acc.begin(), luma_acc.end(), 0u);
        std::fill(chroma_acc.begin(), chroma_acc.end(), 0u);

        // --- Brillo: todos los píxeles del bloque ---
        for (int y = y0; y < y1; ++y) {
            const uint8_t* pr = prev + static_cast<size_t>(y) * static_cast<size_t>(width);
            const uint8_t* cr = y_cur + static_cast<std::ptrdiff_t>(y) * y_rs;
            for (int bx = 0; bx < nbx; ++bx) {
                const int x0 = bx * block;
                const int x1 = std::min(width, x0 + block);
                uint32_t s = 0;
                if (y_ps == 1) {
                    for (int x = x0; x < x1; ++x) {
                        const int d = static_cast<int>(pr[x]) - static_cast<int>(cr[x]);
                        s += static_cast<uint32_t>(d * d);
                    }
                } else {
                    for (int x = x0; x < x1; ++x) {
                        const int d = static_cast<int>(pr[x]) - static_cast<int>(cr[x * y_ps]);
                        s += static_cast<uint32_t>(d * d);
                    }
                }
                luma_acc[bx] += s;
            }
        }

        // --- Color: U y V de todos los píxeles de croma del bloque ---
        const int cy0 = y0 / 2;
        const int cy1 = std::min(ch, y1 / 2);
        for (int cy = cy0; cy < cy1; ++cy) {
            const uint8_t* pr = prev_uv + static_cast<size_t>(cy) * static_cast<size_t>(width);
            const uint8_t* ur = u_cur + static_cast<std::ptrdiff_t>(cy) * u_rs;
            const uint8_t* vr = v_cur + static_cast<std::ptrdiff_t>(cy) * v_rs;
            for (int bx = 0; bx < nbx; ++bx) {
                const int cx0 = (bx * block) / 2;
                const int cx1 = std::min(cw, (std::min(width, bx * block + block)) / 2);
                uint32_t s = 0;
                for (int cx = cx0; cx < cx1; ++cx) {
                    const int du = static_cast<int>(pr[2 * cx]) - static_cast<int>(ur[cx * u_ps]);
                    const int dv = static_cast<int>(pr[2 * cx + 1]) - static_cast<int>(vr[cx * v_ps]);
                    s += static_cast<uint32_t>(du * du + dv * dv);
                }
                chroma_acc[bx] += s;
            }
        }

        // --- Puntuación de cada bloque de esta franja ---
        for (int bx = 0; bx < nbx; ++bx) {
            const int x0 = bx * block;
            const int x1 = std::min(width, x0 + block);
            const int cx0 = x0 / 2;
            const int cx1 = std::min(cw, x1 / 2);
            const int64_t luma_n = static_cast<int64_t>(x1 - x0) * (y1 - y0);
            const int64_t chroma_n = static_cast<int64_t>(std::max(0, cx1 - cx0)) * std::max(0, cy1 - cy0);

            global_luma += luma_acc[bx];
            global_chroma += chroma_acc[bx];

            if (luma_n >= kMinBlockLumaSamples) {
                double score = static_cast<double>(luma_acc[bx]) / static_cast<double>(luma_n);
                if (chroma_n > 0) {
                    score += kChromaWeight * static_cast<double>(chroma_acc[bx]) /
                             (2.0 * static_cast<double>(chroma_n));
                }
                if (score > max_block) max_block = score;
            }
        }

        // Salida anticipada: ya hay un bloque que supera el umbral, el fotograma
        // se conserva sin importar lo que pase en el resto de la imagen.
        if (early_exit > 0.0 && (max_block / block_factor) > early_exit) {
            return max_block / block_factor;
        }
    }

    double global = static_cast<double>(global_luma) /
                    (static_cast<double>(width) * static_cast<double>(height));
    const int64_t chroma_total = static_cast<int64_t>(cw) * ch;
    if (chroma_total > 0) {
        global += kChromaWeight * static_cast<double>(global_chroma) /
                  (2.0 * static_cast<double>(chroma_total));
    }

    return std::max(global, max_block / block_factor);
}

} // namespace

extern "C" {

JNIEXPORT jdouble JNICALL
Java_com_antigravity_deadframeremover_engine_NativeComparator_comparePackedWithYPlane(
    JNIEnv* env,
    jobject /* thiz */,
    jobject packedPrevBuffer,
    jobject currYBuffer,
    jint currYOffset,
    jint currYRowStride,
    jint currYPixelStride,
    jint width,
    jint height
) {
    if (!packedPrevBuffer || !currYBuffer || width <= 0 || height <= 0 || currYOffset < 0) {
        return 999999.0;
    }

    const auto* prev = static_cast<const uint8_t*>(env->GetDirectBufferAddress(packedPrevBuffer));
    const auto* curr_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(currYBuffer));
    if (!prev || !curr_base) {
        return 999999.0;
    }

    const uint8_t* curr = curr_base + currYOffset;

    int sampled_rows = (height + 1) / 2;
    int sampled_cols = (width + 1) / 2;
    uint64_t total_samples = static_cast<uint64_t>(sampled_rows) * sampled_cols;
    if (total_samples == 0) return 0.0;

    uint64_t sum_sq = 0;

    for (int y = 0; y < height; y += 2) {
        const uint8_t* row_p = prev + (y * width);
        const uint8_t* row_c = curr + (y * currYRowStride);
        for (int x = 0; x < width; x += 2) {
            int32_t diff = static_cast<int32_t>(row_p[x]) - static_cast<int32_t>(row_c[x * currYPixelStride]);
            sum_sq += static_cast<uint64_t>(diff * diff);
        }
    }

    return static_cast<jdouble>(sum_sq) / static_cast<double>(total_samples);
}

JNIEXPORT jint JNICALL
Java_com_antigravity_deadframeremover_engine_NativeComparator_packYPlane(
    JNIEnv* env,
    jobject /* thiz */,
    jobject currYBuffer,
    jint currYOffset,
    jint currYRowStride,
    jint currYPixelStride,
    jint width,
    jint height,
    jobject packedDstBuffer
) {
    if (!currYBuffer || !packedDstBuffer || width <= 0 || height <= 0 || currYOffset < 0) {
        return -1;
    }

    const auto* src_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(currYBuffer));
    auto* dst = static_cast<uint8_t*>(env->GetDirectBufferAddress(packedDstBuffer));
    if (!src_base || !dst) return -2;

    const uint8_t* src = src_base + currYOffset;

    for (int y = 0; y < height; ++y) {
        const uint8_t* src_row = src + (y * currYRowStride);
        uint8_t* dst_row = dst + (y * width);
        if (currYPixelStride == 1) {
            std::memcpy(dst_row, src_row, width);
        } else {
            for (int x = 0; x < width; ++x) {
                dst_row[x] = src_row[x * currYPixelStride];
            }
        }
    }
    return 0;
}

JNIEXPORT jdouble JNICALL
Java_com_antigravity_deadframeremover_engine_NativeComparator_compareYUVPlanes(
    JNIEnv* env,
    jobject /* thiz */,
    jobject bufferPrev,
    jint prevOffset,
    jobject bufferCurr,
    jint currOffset,
    jint width,
    jint height,
    jint yRowStride,
    jint yPixelStride,
    jdouble /* threshold */
) {
    if (!bufferPrev || !bufferCurr || width <= 0 || height <= 0 || prevOffset < 0 || currOffset < 0) {
        return 999999.0;
    }

    const auto* prev_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(bufferPrev));
    const auto* curr_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(bufferCurr));
    if (!prev_base || !curr_base) {
        return 999999.0;
    }

    const uint8_t* prev = prev_base + prevOffset;
    const uint8_t* curr = curr_base + currOffset;

    int sampled_rows = (height + 1) / 2;
    int sampled_cols = (width + 1) / 2;
    uint64_t total_samples = static_cast<uint64_t>(sampled_rows) * sampled_cols;
    if (total_samples == 0) return 0.0;

    uint64_t sum_sq = 0;

    for (int y = 0; y < height; y += 2) {
        const uint8_t* row_p = prev + (y * yRowStride);
        const uint8_t* row_c = curr + (y * yRowStride);
        for (int x = 0; x < width; x += 2) {
            int32_t diff = static_cast<int32_t>(row_p[x * yPixelStride]) - static_cast<int32_t>(row_c[x * yPixelStride]);
            sum_sq += static_cast<uint64_t>(diff * diff);
        }
    }

    return static_cast<jdouble>(sum_sq) / static_cast<double>(total_samples);
}

JNIEXPORT jint JNICALL
Java_com_antigravity_deadframeremover_engine_NativeComparator_normalizeYUV420ToNV12(
    JNIEnv* env,
    jobject /* thiz */,
    jobject yBuffer, jint yOffset, jint yRowStride, jint yPixelStride,
    jobject uBuffer, jint uOffset, jint uRowStride, jint uPixelStride,
    jobject vBuffer, jint vOffset, jint vRowStride, jint vPixelStride,
    jobject dstBuffer, jint dstOffset,
    jint width, jint height
) {
    if (!yBuffer || !uBuffer || !vBuffer || !dstBuffer) return -1;
    if (width <= 0 || height <= 0 || yOffset < 0 || uOffset < 0 || vOffset < 0 || dstOffset < 0) return -2;

    const auto* y_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    const auto* u_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(uBuffer));
    const auto* v_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(vBuffer));
    auto* dst_base     = static_cast<uint8_t*>(env->GetDirectBufferAddress(dstBuffer));

    if (!y_base || !u_base || !v_base || !dst_base) return -3;

    const uint8_t* y_src = y_base + yOffset;
    const uint8_t* u_src = u_base + uOffset;
    const uint8_t* v_src = v_base + vOffset;
    uint8_t* dst         = dst_base + dstOffset;

    // 1. Explicit Y Plane Packing (width * height)
    uint8_t* dst_y = dst;
    for (int r = 0; r < height; ++r) {
        const uint8_t* src_row = y_src + (r * yRowStride);
        uint8_t* dst_row = dst_y + (r * width);
        if (yPixelStride == 1) {
            std::memcpy(dst_row, src_row, width);
        } else {
            for (int c = 0; c < width; ++c) {
                dst_row[c] = src_row[c * yPixelStride];
            }
        }
    }

    // 2. Explicit UV Interleaved Plane Packing (width * (height / 2))
    uint8_t* dst_uv = dst + (width * height);
    int uv_height = height / 2;
    int uv_width  = width / 2;
    for (int r = 0; r < uv_height; ++r) {
        const uint8_t* u_row = u_src + (r * uRowStride);
        const uint8_t* v_row = v_src + (r * vRowStride);
        uint8_t* dst_row = dst_uv + (r * width);
        for (int c = 0; c < uv_width; ++c) {
            dst_row[2 * c]     = u_row[c * uPixelStride];
            dst_row[2 * c + 1] = v_row[c * vPixelStride];
        }
    }

    return 0;
}

JNIEXPORT jint JNICALL
Java_com_antigravity_deadframeremover_engine_NativeComparator_yuvToRgbBitmap(
    JNIEnv* env,
    jobject /* thiz */,
    jobject yBuffer, jint yOffset, jint yRowStride, jint yPixelStride,
    jobject uBuffer, jint uOffset, jint uRowStride, jint uPixelStride,
    jobject vBuffer, jint vOffset, jint vRowStride, jint vPixelStride,
    jint srcWidth, jint srcHeight,
    jobject dstBitmap
) {
    if (!yBuffer || !uBuffer || !vBuffer || !dstBitmap) return -1;
    if (srcWidth <= 0 || srcHeight <= 0 || yOffset < 0 || uOffset < 0 || vOffset < 0) return -2;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, dstBitmap, &info) < 0) return -3;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) return -4;

    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, dstBitmap, &pixels) < 0 || !pixels) return -5;

    const auto* y_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    const auto* u_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(uBuffer));
    const auto* v_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(vBuffer));

    if (!y_base || !u_base || !v_base) {
        AndroidBitmap_unlockPixels(env, dstBitmap);
        return -6;
    }

    const uint8_t* y_src = y_base + yOffset;
    const uint8_t* u_src = u_base + uOffset;
    const uint8_t* v_src = v_base + vOffset;

    int dstWidth  = static_cast<int>(info.width);
    int dstHeight = static_cast<int>(info.height);
    uint32_t strideBytes = info.stride;

    for (int dy = 0; dy < dstHeight; ++dy) {
        int sy = (dy * srcHeight) / dstHeight;
        int uv_y = (sy / 2);
        const uint8_t* row_y = y_src + (sy * yRowStride);
        const uint8_t* row_u = u_src + (uv_y * uRowStride);
        const uint8_t* row_v = v_src + (uv_y * vRowStride);

        auto* out_row = reinterpret_cast<uint8_t*>(pixels) + (dy * strideBytes);

        for (int dx = 0; dx < dstWidth; ++dx) {
            int sx = (dx * srcWidth) / dstWidth;
            int uv_x = (sx / 2);

            int y_val = static_cast<int>(row_y[sx * yPixelStride]);
            int u_val = static_cast<int>(row_u[uv_x * uPixelStride]);
            int v_val = static_cast<int>(row_v[uv_x * vPixelStride]);

            int c = y_val - 16;
            int d = u_val - 128;
            int e = v_val - 128;

            int r = (298 * c + 409 * e + 128) >> 8;
            int g = (298 * c - 100 * d - 208 * e + 128) >> 8;
            int b = (298 * c + 516 * d + 128) >> 8;

            out_row[dx * 4 + 0] = static_cast<uint8_t>(std::clamp(r, 0, 255));
            out_row[dx * 4 + 1] = static_cast<uint8_t>(std::clamp(g, 0, 255));
            out_row[dx * 4 + 2] = static_cast<uint8_t>(std::clamp(b, 0, 255));
            out_row[dx * 4 + 3] = 0xFF;
        }
    }

    AndroidBitmap_unlockPixels(env, dstBitmap);
    return 0;
}

// Modo "Preciso": compara el fotograma actual (planos Y, U y V del decodificador)
// contra el último fotograma conservado, guardado como NV12 empaquetado
// (se rellena con normalizeYUV420ToNV12 usando dstOffset = 0; el búfer debe
// tener al menos width * height * 3 / 2 bytes).
//
// Devuelve la puntuación para comparar con el umbral. Si earlyExit > 0 y ya hay
// un bloque que lo supera, devuelve antes de revisar toda la imagen (la cifra es
// entonces un mínimo, suficiente para saber que el fotograma NO es duplicado).
JNIEXPORT jdouble JNICALL
Java_com_antigravity_deadframeremover_engine_NativeComparator_comparePreciseNV12(
    JNIEnv* env,
    jobject /* thiz */,
    jobject prevNv12Buffer,
    jobject yBuffer, jint yOffset, jint yRowStride, jint yPixelStride,
    jobject uBuffer, jint uOffset, jint uRowStride, jint uPixelStride,
    jobject vBuffer, jint vOffset, jint vRowStride, jint vPixelStride,
    jint width, jint height,
    jint sensitivity,
    jdouble earlyExit
) {
    if (!prevNv12Buffer || !yBuffer || !uBuffer || !vBuffer) return 999999.0;
    if (width < 2 || height < 2) return 999999.0;
    if (yOffset < 0 || uOffset < 0 || vOffset < 0) return 999999.0;
    if (yPixelStride < 1 || uPixelStride < 1 || vPixelStride < 1) return 999999.0;

    const auto* prev = static_cast<const uint8_t*>(env->GetDirectBufferAddress(prevNv12Buffer));
    const auto* y_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    const auto* u_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(uBuffer));
    const auto* v_base = static_cast<const uint8_t*>(env->GetDirectBufferAddress(vBuffer));
    if (!prev || !y_base || !u_base || !v_base) return 999999.0;

    int level = static_cast<int>(sensitivity);
    if (level < 0) level = 0;
    if (level > 2) level = 2;

    return ComparePreciseCore(
        prev,
        y_base + yOffset, static_cast<int>(yRowStride), static_cast<int>(yPixelStride),
        u_base + uOffset, static_cast<int>(uRowStride), static_cast<int>(uPixelStride),
        v_base + vOffset, static_cast<int>(vRowStride), static_cast<int>(vPixelStride),
        static_cast<int>(width), static_cast<int>(height),
        kPreciseLevels[level].block, kPreciseLevels[level].factor,
        static_cast<double>(earlyExit)
    );
}

} // extern "C"
