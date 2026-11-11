package com.expensetracker.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max

object ImageUtils {

    const val MAX_OCR_DIMENSION = 2048

    /**
     * Tạo Uri tạm thời cho Camera chụp ảnh độ phân giải gốc cao nhất.
     * Tự động dọn dẹp các tệp ảnh chụp tạm cũ (trên 2 tiếng) để tiết kiệm dung lượng bộ nhớ thiết bị.
     */
    fun createCameraTempUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "camera")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        } else {
            cleanOldCameraCache(cacheDir)
        }
        val file = File(cacheDir, "receipt_capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Dọn dẹp các tệp tạm thời chụp ảnh hóa đơn cũ hơn 2 giờ để tránh đầy bộ nhớ máy
     */
    fun cleanOldCameraCache(cacheDir: File) {
        try {
            val threshold = System.currentTimeMillis() - (2 * 60 * 60 * 1000L) // 2 giờ
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < threshold) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Đọc ảnh từ Uri từ Thư viện hoặc Camera:
     * - Hỗ trợ toàn bộ định dạng: JPG, PNG, WEBP, HEIC/HEIF (từ iPhone, Samsung, Pixel).
     * - Tự động sửa góc xoay (EXIF rotation) để hóa đơn luôn thẳng đứng.
     * - Đọc qua ImageDecoder (Android 9+) không bị lỗi stream từ Google Photos/Cloud.
     * - Có cơ chế Fallback chép tạm file cache nếu stream bị giới hạn quyền truy cập.
     */
    fun loadAndOptimizeBitmap(context: Context, uri: Uri): Bitmap? {
        // Chiến lược 1: Sử dụng ImageDecoder (Android 9 trở lên) - xử lý mượt mà HEIC, Google Photos và tự động xoay EXIF
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxDim = max(info.size.width, info.size.height)
                    if (maxDim > MAX_OCR_DIMENSION) {
                        val sampleSize = (maxDim / MAX_OCR_DIMENSION).coerceAtLeast(1)
                        if (sampleSize >= 2) {
                            decoder.setTargetSampleSize(sampleSize)
                        }
                    }
                }
                return optimizeBitmap(decoded)
            } catch (e: Exception) {
                e.printStackTrace()
                // Thất bại với ImageDecoder thì chuyển tiếp xuống Chiến lược 2
            }
        }

        // Chiến lược 2: Sao chép luồng stream ra tệp tạm nội bộ của app (tránh lỗi permission denied hay stream đóng sớm của ContentResolver)
        return try {
            val cacheFile = File(context.cacheDir, "camera/temp_gallery_${System.currentTimeMillis()}.tmp")
            cacheFile.parentFile?.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val rotationDegrees = try {
                val exif = ExifInterface(cacheFile.absolutePath)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (e: Exception) {
                0
            }

            // Đọc kích thước
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(cacheFile.absolutePath, boundsOptions)
            val maxDim = max(boundsOptions.outWidth, boundsOptions.outHeight)

            var sampleSize = 1
            while (maxDim / (sampleSize * 2) >= MAX_OCR_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeFile(cacheFile.absolutePath, decodeOptions)

            // Dọn dẹp tệp tạm
            try { cacheFile.delete() } catch (_: Exception) {}

            if (sampledBitmap == null) return null

            val currentMaxDim = max(sampledBitmap.width, sampledBitmap.height)
            val scale = if (currentMaxDim > MAX_OCR_DIMENSION) {
                MAX_OCR_DIMENSION.toFloat() / currentMaxDim.toFloat()
            } else {
                1.0f
            }

            if (rotationDegrees != 0 || scale < 1.0f) {
                val matrix = Matrix().apply {
                    if (scale < 1.0f) postScale(scale, scale)
                    if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
                }
                val transformed = Bitmap.createBitmap(
                    sampledBitmap, 0, 0, sampledBitmap.width, sampledBitmap.height, matrix, true
                )
                if (transformed != sampledBitmap) {
                    sampledBitmap.recycle()
                }
                transformed
            } else {
                sampledBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Tối ưu Bitmap đã nạp sẵn trong bộ nhớ về chuẩn kích thước OCR tối đa 2048px
     */
    fun optimizeBitmap(bitmap: Bitmap): Bitmap {
        val currentMaxDim = max(bitmap.width, bitmap.height)
        if (currentMaxDim <= MAX_OCR_DIMENSION) return bitmap

        val scale = MAX_OCR_DIMENSION.toFloat() / currentMaxDim.toFloat()
        val matrix = Matrix().apply { postScale(scale, scale) }
        val scaled = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }
}
