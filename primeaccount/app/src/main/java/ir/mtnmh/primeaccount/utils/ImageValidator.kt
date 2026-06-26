package ir.mtnmh.primeaccount.utils

import android.content.Context
import android.net.Uri
import android.util.Log

object ImageValidator {
    private const val TAG = "ImageValidator"
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB

    data class ValidationResult(val isValid: Boolean, val reason: String)

    fun validateImage(context: Context, uriString: String?): ValidationResult {
        if (uriString.isNullOrEmpty()) {
            return ValidationResult(false, "مسیر فایل خالی است / Image path is empty")
        }

        // If it is already an HTTP URL, it's valid
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return ValidationResult(true, "آدرس اینترنتی معتبر / Valid web URL")
        }

        val uri = try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            return ValidationResult(false, "مسیر نامعتبر / Invalid URI format")
        }

        // Validate Scheme
        val scheme = uri.scheme
        if (scheme != "content" && scheme != "file") {
            return ValidationResult(false, "فرمت منبع نامعتبر / Unsupported URI scheme")
        }

        // Check file type and size using ContentResolver
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            
            // Check file type if MIME is provided
            if (mimeType.isNotEmpty() && !mimeType.startsWith("image/")) {
                return ValidationResult(false, "نوع فایل نامعتبر است. فقط تصویر مجاز است / Unsupported file type: $mimeType")
            }

            // Check size
            var fileSize: Long = -1
            if (scheme == "content") {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1 && cursor.moveToFirst()) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            } else if (scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        fileSize = file.length()
                    }
                }
            }

            // Fallback stream size check if size is still undetermined
            if (fileSize <= 0) {
                contentResolver.openInputStream(uri)?.use { stream ->
                    fileSize = stream.available().toLong()
                }
            }

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return ValidationResult(false, "حجم تصویر نباید بیشتر از ۱۰ مگابایت باشد / Image size must be less than 10MB")
            }

            return ValidationResult(true, "تایید شد / Image is valid")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied accessing image: ${e.message}")
            return ValidationResult(false, "عدم دسترسی به فایل تصویر / Permission denied accessing image")
        } catch (e: Exception) {
            Log.e(TAG, "Error validating image: ${e.message}")
            // Check extension as fallback
            val extension = MimeTypeMapGetFileExtension(uriString)
            val isSupportedExt = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp").contains(extension.lowercase())
            if (isSupportedExt) {
                return ValidationResult(true, "تایید شد (پسوند معتبر) / Image is valid extension fallback")
            }
            return ValidationResult(false, "خطا در پردازش تصویر / Error processing image")
        }
    }

    private fun MimeTypeMapGetFileExtension(url: String): String {
        val lastDot = url.lastIndexOf('.')
        if (lastDot != -1 && lastDot < url.length - 1) {
            return url.substring(lastDot + 1)
        }
        return ""
    }
}
