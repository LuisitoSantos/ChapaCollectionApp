// Archivo: utils/ImageUtils.kt
package com.tuempresa.chapacollectionapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

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

/*
fun recortarImagenVisibleDesdeUri(
    context: Context,
    imageUri: Uri,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    frameSizePx: Float,
    maskRadiusPx: Float? = null
): Uri? {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        var bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (bitmap == null) return null

        // Asegurarnos de que la orientación está correcta según EXIF (especialmente para fotos de cámara)
        bitmap = rotateBitmapIfRequired(context, imageUri, bitmap)

        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()

        // Calculamos el factor con el que la imagen original se ajusta al marco (ContentScale.Fit)
        val scaleToDisplay = frameSizePx / maxOf(originalWidth, originalHeight)

        // Tamaño mostrado antes del zoom
        val displayedWidth = originalWidth * scaleToDisplay
        val displayedHeight = originalHeight * scaleToDisplay

        // Tamaño mostrado después del zoom del usuario
        val displayedWidthZoom = displayedWidth * scale
        val displayedHeightZoom = displayedHeight * scale

        // Posición inicial (centrada) del bitmap dentro del frame antes de aplicar offset
        val initialLeft = (frameSizePx - displayedWidthZoom) / 2f
        val initialTop = (frameSizePx - displayedHeightZoom) / 2f

        // La offset proviene del pointerInput (píxeles en la vista). Usamos directamente para desplazar la imagen.
        val translateX = offset.x
        val translateY = offset.y

        val destLeft = initialLeft + translateX
        val destTop = initialTop + translateY
        val destRight = destLeft + displayedWidthZoom
        val destBottom = destTop + displayedHeightZoom

        // Crear bitmap de salida (cuadrado del frame) y dibujar la imagen escalada/trasladada exactamente como en la vista
        val outSize = kotlin.math.round(frameSizePx).toInt()
        val outputBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        // Fondo blanco
        canvas.drawColor(AndroidColor.WHITE)

        // Rect destino donde dibujar la imagen (float para evitar truncamientos)
        val destRect = RectF(destLeft, destTop, destRight, destBottom)

        // Dibujar la imagen escalada en el rect destino
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, destRect, paint)

        // Aplicar máscara circular usando saveLayer + PorterDuff SRC_IN para recortar con antialias correcto
        val finalBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalBitmap)
        finalCanvas.drawColor(AndroidColor.WHITE)

        val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, outSize.toFloat(), outSize.toFloat())
        val cx = outSize / 2f
        val cy = outSize / 2f
        // La guía en OverlayCuadradoConGuiaCircular usa radius = marcoSize / 2.2f y un grosor por defecto de 1.dp.
        // Para que el recorte coincida exactamente con la parte interior de la guía (lo que el usuario ve dentro),
        // restamos la mitad del grosor del trazo (stroke) al radio.
        val density = context.resources.displayMetrics.density
        val radius = maskRadiusPx ?: run {
            val guiaGrosorDp = 1f // valor por defecto en OverlayCuadradoConGuiaCircular
            val guiaGrosorPx = guiaGrosorDp * density
            // Substraer mitad de grosor + pequeño epsilon para evitar halos/aliasing
            val epsilon = 0.5f
            (frameSizePx / 2.2f) - (guiaGrosorPx / 2f) - epsilon
        }

        finalCanvas.saveLayer(rect, layerPaint)
        // Dibujamos la máscara (círculo) en la capa con antialias
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.isAntiAlias = true
        maskPaint.color = AndroidColor.BLACK
        finalCanvas.drawCircle(cx, cy, radius, maskPaint)

        // Ahora establecemos Xfermode para mantener solo la parte de la imagen dentro del círculo
        val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        xferPaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        finalCanvas.drawBitmap(outputBitmap, 0f, 0f, xferPaint)
        xferPaint.xfermode = null
        finalCanvas.restore()

        val outputFile = File(context.cacheDir, "chapa_crop_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(outputFile)
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()

        return outputFile.toUri()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
*/

fun recortarImagenVisibleDesdeUri(
    context: Context,
    imageUri: Uri,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    frameSizePx: Float,
    maskRadiusPx: Float? = null
): Uri? {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        var bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (bitmap == null) return null

        bitmap = rotateBitmapIfRequired(context, imageUri, bitmap)

        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()
        val scaleToDisplay = frameSizePx / maxOf(originalWidth, originalHeight)

        val displayedWidthZoom = originalWidth * scaleToDisplay * scale
        val displayedHeightZoom = originalHeight * scaleToDisplay * scale

        val destLeft = (frameSizePx - displayedWidthZoom) / 2f + offset.x
        val destTop = (frameSizePx - displayedHeightZoom) / 2f + offset.y

        val outSize = kotlin.math.round(frameSizePx).toInt()

        // 1. Creamos el bitmap temporal de la imagen posicionada
        val outputBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        // ELIMINADO: canvas.drawColor(AndroidColor.WHITE) -> Queremos transparencia

        val destRect = RectF(destLeft, destTop, destLeft + displayedWidthZoom, destTop + displayedHeightZoom)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, destRect, paint)

        // 2. Creamos el bitmap final con el recorte circular
        val finalBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalBitmap)

        val cx = outSize / 2f
        val cy = outSize / 2f
        val density = context.resources.displayMetrics.density
        val radius = maskRadiusPx ?: ((frameSizePx / 2.2f) - (1f * density / 2f) - 0.5f)

        // Aplicar máscara circular
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.BLACK }
        finalCanvas.drawCircle(cx, cy, radius, maskPaint)

        val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        finalCanvas.drawBitmap(outputBitmap, 0f, 0f, xferPaint)

        // 3. DIBUJAR EL BORDE NEGRO
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f * density // Grosor del borde (2dp)
        }
        finalCanvas.drawCircle(cx, cy, radius - (borderPaint.strokeWidth / 2f), borderPaint)

        // 4. GUARDAR COMO PNG (Obligatorio para transparencia)
        val outputFile = File(context.cacheDir, "chapa_crop_${System.currentTimeMillis()}.png")
        val outputStream = FileOutputStream(outputFile)
        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()

        return outputFile.toUri()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

/*
fun recortarImagenVisibleDesdeBitmap(
    context: Context,
    bitmap: Bitmap,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    frameSizePx: Float,
    maskRadiusPx: Float? = null
): Uri? {
    try {
        val bmp = bitmap // bitmap ya debería estar rotado si procede

        val originalWidth = bmp.width.toFloat()
        val originalHeight = bmp.height.toFloat()

        // Calculamos el factor con el que la imagen original se ajusta al marco (ContentScale.Fit)
        val scaleToDisplay = frameSizePx / maxOf(originalWidth, originalHeight)

        // Tamaño mostrado antes del zoom
        val displayedWidth = originalWidth * scaleToDisplay
        val displayedHeight = originalHeight * scaleToDisplay

        // Tamaño mostrado después del zoom del usuario
        val displayedWidthZoom = displayedWidth * scale
        val displayedHeightZoom = displayedHeight * scale

        // Posición inicial (centrada) del bitmap dentro del frame antes de aplicar offset
        val initialLeft = (frameSizePx - displayedWidthZoom) / 2f
        val initialTop = (frameSizePx - displayedHeightZoom) / 2f

        // La offset proviene del pointerInput (píxeles en la vista). Usamos directamente para desplazar la imagen.
        val translateX = offset.x
        val translateY = offset.y

        val destLeft = initialLeft + translateX
        val destTop = initialTop + translateY
        val destRight = destLeft + displayedWidthZoom
        val destBottom = destTop + displayedHeightZoom

        // Crear bitmap de salida (cuadrado del frame) y dibujar la imagen escalada/trasladada exactamente como en la vista
        val outSize = kotlin.math.round(frameSizePx).toInt()
        val outputBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        // Fondo blanco
        canvas.drawColor(AndroidColor.WHITE)

        // Rect destino donde dibujar la imagen (float para evitar truncamientos)
        val destRect = RectF(destLeft, destTop, destRight, destBottom)

        // Dibujar la imagen escalada en el rect destino
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bmp, null, destRect, paint)

        // Aplicar máscara circular usando saveLayer + PorterDuff SRC_IN
        val finalBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalBitmap)
        finalCanvas.drawColor(AndroidColor.WHITE)

        val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, outSize.toFloat(), outSize.toFloat())
        val cx = outSize / 2f
        val cy = outSize / 2f
        val density2 = context.resources.displayMetrics.density
        val radius = maskRadiusPx ?: run {
            val guiaGrosorPx2 = 1f * density2
            val epsilon = 0.5f
            (frameSizePx / 2.2f) - (guiaGrosorPx2 / 2f) - epsilon
        }

        finalCanvas.saveLayer(rect, layerPaint)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.isAntiAlias = true
        maskPaint.color = AndroidColor.BLACK
        finalCanvas.drawCircle(cx, cy, radius, maskPaint)

        val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        xferPaint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        finalCanvas.drawBitmap(outputBitmap, 0f, 0f, xferPaint)
        xferPaint.xfermode = null
        finalCanvas.restore()

        val outputFile = File(context.cacheDir, "chapa_crop_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(outputFile)
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()

        return outputFile.toUri()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
 */

fun recortarImagenVisibleDesdeBitmap(
    context: Context,
    bitmap: Bitmap,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    frameSizePx: Float,
    maskRadiusPx: Float? = null
): Uri? {
    try {
        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()
        val scaleToDisplay = frameSizePx / maxOf(originalWidth, originalHeight)

        val displayedWidthZoom = originalWidth * scaleToDisplay * scale
        val displayedHeightZoom = originalHeight * scaleToDisplay * scale

        val destLeft = (frameSizePx - displayedWidthZoom) / 2f + offset.x
        val destTop = (frameSizePx - displayedHeightZoom) / 2f + offset.y

        val outSize = kotlin.math.round(frameSizePx).toInt()
        val outputBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        val destRect = RectF(destLeft, destTop, destLeft + displayedWidthZoom, destTop + displayedHeightZoom)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, destRect, paint)

        val finalBitmap = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
        val finalCanvas = Canvas(finalBitmap)

        val cx = outSize / 2f
        val cy = outSize / 2f
        val density = context.resources.displayMetrics.density
        val radius = maskRadiusPx ?: ((frameSizePx / 2.2f) - (1f * density / 2f) - 0.5f)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.BLACK }
        finalCanvas.drawCircle(cx, cy, radius, maskPaint)

        val xferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        finalCanvas.drawBitmap(outputBitmap, 0f, 0f, xferPaint)

        // BORDE NEGRO
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        finalCanvas.drawCircle(cx, cy, radius - (borderPaint.strokeWidth / 2f), borderPaint)

        // GUARDAR COMO PNG
        val outputFile = File(context.cacheDir, "chapa_crop_${System.currentTimeMillis()}.png")
        val outputStream = FileOutputStream(outputFile)
        finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()

        return outputFile.toUri()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
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
