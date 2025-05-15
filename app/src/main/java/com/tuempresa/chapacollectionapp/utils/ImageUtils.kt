// Archivo: utils/ImageUtils.kt
package com.tuempresa.chapacollectionapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.geometry.Offset
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream

/**
 * Crea una URI segura para guardar una imagen capturada por la cámara.
 */
fun createImageUri(context: Context): Uri {
    val imageFile = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "chapa_${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

fun cropCenterSquare(bitmap: Bitmap): Bitmap {
    val dimension = minOf(bitmap.width, bitmap.height)
    val xOffset = (bitmap.width - dimension) / 2
    val yOffset = (bitmap.height - dimension) / 2
    return Bitmap.createBitmap(bitmap, xOffset, yOffset, dimension, dimension)
}

fun recortarImagenVisibleDesdeUri(
    context: Context,
    uri: Uri,
    scale: Float,
    offset: Offset,
    frameSizePx: Float = 512f
): Uri? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

    val scaledWidth = originalBitmap.width * scale
    val scaledHeight = originalBitmap.height * scale

    val left = ((scaledWidth - frameSizePx) / 2f - offset.x).coerceAtLeast(0f)
    val top = ((scaledHeight - frameSizePx) / 2f - offset.y).coerceAtLeast(0f)

    val matrix = Matrix().apply {
        postScale(scale, scale)
    }

    val scaledBitmap = Bitmap.createBitmap(
        originalBitmap,
        0, 0,
        originalBitmap.width, originalBitmap.height,
        matrix, true
    )

    val cropped = Bitmap.createBitmap(
        scaledBitmap,
        left.toInt().coerceAtMost(scaledBitmap.width - frameSizePx.toInt()),
        top.toInt().coerceAtMost(scaledBitmap.height - frameSizePx.toInt()),
        frameSizePx.toInt(),
        frameSizePx.toInt()
    )

    val file = File(context.cacheDir, "chapa_recortada_${System.currentTimeMillis()}.jpg")
    val output = FileOutputStream(file)
    cropped.compress(Bitmap.CompressFormat.JPEG, 90, output)
    output.close()

    return file.toUri()
}

fun rotateBitmapIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val exif = inputStream?.let { ExifInterface(it) }
        inputStream?.close()

        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        bitmap // Si algo falla, devuelve el original
    }
}
