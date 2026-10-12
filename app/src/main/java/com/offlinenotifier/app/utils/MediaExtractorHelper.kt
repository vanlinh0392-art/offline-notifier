package com.offlinenotifier.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object MediaExtractorHelper {
    private const val TAG = "MediaExtractorHelper"
    private const val MAX_IMAGE_DIMENSION = 1024
    private const val JPEG_QUALITY = 60
    private const val MAX_AUDIO_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB limit

    /**
     * Nén và lưu Bitmap xuống bộ nhớ máy với kích thước tối đa maxWidth và chất lượng JPEG 60%.
     */
    fun compressAndSaveBitmap(
        context: Context,
        original: Bitmap,
        maxWidth: Int = MAX_IMAGE_DIMENSION,
        quality: Int = JPEG_QUALITY
    ): String? {
        return try {
            val width = original.width
            val height = original.height

            val targetBitmap = if (width > maxWidth || height > maxWidth) {
                val scale = min(maxWidth.toFloat() / width, maxWidth.toFloat() / height)
                val newWidth = (width * scale).toInt().coerceAtLeast(1)
                val newHeight = (height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
            } else {
                original
            }

            val imageDir = File(context.filesDir, "notification_images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            val imageFile = File(imageDir, "noti_img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(imageFile).use { fos ->
                targetBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }

            if (targetBitmap != original && !targetBitmap.isRecycled) {
                targetBitmap.recycle()
            }

            Log.d(TAG, "Compressed and saved image: ${imageFile.absolutePath} (${imageFile.length() / 1024} KB)")
            imageFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress and save bitmap", e)
            null
        }
    }

    /**
     * Trích xuất hình ảnh từ Notification Extras (BigPicture, PictureIcon, LargeIcon, MessagingStyle).
     */
    fun extractImageFromExtras(context: Context, extras: Bundle): String? {
        try {
            // 1. Trích xuất từ "android.picture" (Bitmap)
            @Suppress("DEPRECATION")
            val pictureBitmap = extras.getParcelable<Bitmap>("android.picture")
            if (pictureBitmap != null) {
                return compressAndSaveBitmap(context, pictureBitmap)
            }

            // 2. Trích xuất từ "android.pictureIcon" (Android 12+ Icon)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                val pictureIcon = extras.getParcelable<Icon>("android.pictureIcon")
                if (pictureIcon != null) {
                    val drawable = pictureIcon.loadDrawable(context)
                    if (drawable is BitmapDrawable && drawable.bitmap != null) {
                        return compressAndSaveBitmap(context, drawable.bitmap)
                    }
                }
            }

            // 3. Trích xuất từ "android.messages" (MessagingStyle messages with image URI)
            @Suppress("DEPRECATION")
            val messages = extras.getParcelableArray("android.messages")
            if (messages != null) {
                for (msgObj in messages.reversed()) {
                    if (msgObj is Bundle) {
                        val mimeType = msgObj.getString("type") ?: msgObj.getString("dataMimeType")
                        val dataUri = msgObj.getParcelable<Uri>("uri")
                            ?: msgObj.getParcelable<Uri>("dataUri")
                            ?: msgObj.getString("uri")?.let { Uri.parse(it) }

                        if (mimeType?.startsWith("image/") == true && dataUri != null) {
                            try {
                                context.contentResolver.openInputStream(dataUri)?.use { input ->
                                    val decoded = BitmapFactory.decodeStream(input)
                                    if (decoded != null) {
                                        val saved = compressAndSaveBitmap(context, decoded)
                                        if (!decoded.isRecycled) decoded.recycle()
                                        if (saved != null) return saved
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Cannot read image uri: $dataUri", e)
                            }
                        }
                    }
                }
            }

            // 4. Trích xuất từ "android.largeIcon.big" hoặc "android.largeIcon" (nếu có ảnh lớn)
            @Suppress("DEPRECATION")
            val largeIconBitmap = extras.getParcelable<Bitmap>("android.largeIcon.big")
                ?: extras.getParcelable<Bitmap>("android.largeIcon")
            if (largeIconBitmap != null && largeIconBitmap.width >= 120 && largeIconBitmap.height >= 120) {
                return compressAndSaveBitmap(context, largeIconBitmap)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting image from notification", e)
        }
        return null
    }

    /**
     * Trích xuất file Audio (Tin nhắn thoại / file âm thanh) từ Notification Extras.
     */
    fun extractAudioFromExtras(context: Context, extras: Bundle): String? {
        try {
            // Duyệt qua "android.messages" và "android.messages.historic" (MessagingStyle)
            val messageKeys = arrayOf("android.messages", "android.messages.historic")
            for (key in messageKeys) {
                @Suppress("DEPRECATION")
                val messages = extras.getParcelableArray(key) ?: continue
                for (msgObj in messages.reversed()) {
                    if (msgObj is Bundle) {
                        val mimeType = msgObj.getString("type") ?: msgObj.getString("dataMimeType")
                        val dataUri = msgObj.getParcelable<Uri>("uri")
                            ?: msgObj.getParcelable<Uri>("dataUri")
                            ?: msgObj.getString("uri")?.let { Uri.parse(it) }

                        if (mimeType != null && mimeType.startsWith("audio/") && dataUri != null) {
                            val savedAudio = copyAudioUriToFile(context, dataUri, mimeType)
                            if (savedAudio != null) {
                                return savedAudio
                            }
                        }
                    }
                }
            }

            // Kiểm tra "android.audioURI"
            val audioUriString = extras.getString("android.audioURI")
            if (!audioUriString.isNullOrBlank()) {
                val uri = Uri.parse(audioUriString)
                val saved = copyAudioUriToFile(context, uri, "audio/mpeg")
                if (saved != null) return saved
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting audio from notification", e)
        }
        return null
    }

    private fun copyAudioUriToFile(context: Context, uri: Uri, mimeType: String): String? {
        return try {
            val extension = getExtensionFromMime(mimeType)
            val audioDir = File(context.filesDir, "notification_audios")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val audioFile = File(audioDir, "noti_audio_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(audioFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (totalRead > MAX_AUDIO_SIZE_BYTES) {
                            Log.w(TAG, "Audio file exceeds max size limit ($MAX_AUDIO_SIZE_BYTES bytes). Aborting.")
                            audioFile.delete()
                            return null
                        }
                    }
                }
            }

            if (audioFile.exists() && audioFile.length() > 0) {
                Log.d(TAG, "Extracted audio file: ${audioFile.absolutePath} (${audioFile.length() / 1024} KB, mime=$mimeType)")
                audioFile.absolutePath
            } else {
                audioFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy audio URI $uri to file", e)
            null
        }
    }

    private fun getExtensionFromMime(mimeType: String): String {
        return when {
            mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
            mimeType.contains("ogg") || mimeType.contains("opus") -> "ogg"
            mimeType.contains("aac") -> "aac"
            mimeType.contains("amr") -> "amr"
            mimeType.contains("wav") -> "wav"
            mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
            else -> "mp3"
        }
    }

    /**
     * Xóa an toàn file media (ảnh hoặc audio).
     */
    fun deleteMediaFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Deleted media file: $filePath (success=$deleted)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting media file: $filePath", e)
        }
    }

    /**
     * Tự động quét và dọn dẹp các file media tạm thời bị mồ côi cũ hơn 24 giờ.
     * Tránh tình trạng rò rỉ dung lượng bộ nhớ máy khi app bị Force Stop giữa chừng.
     */
    fun purgeOrphanMediaFiles(context: Context) {
        try {
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            val directories = listOf("notification_images", "notification_audios")
            var deletedCount = 0
            for (dirName in directories) {
                val dir = File(context.filesDir, dirName)
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    for (file in files) {
                        if (file.lastModified() < cutoff) {
                            if (file.delete()) {
                                deletedCount++
                            }
                        }
                    }
                }
            }
            if (deletedCount > 0) {
                Log.d(TAG, "Session GC: Purged $deletedCount orphan media files older than 24h.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error purging orphan media files", e)
        }
    }
}
