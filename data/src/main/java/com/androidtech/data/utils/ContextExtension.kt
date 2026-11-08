package com.androidtech.data.utils

import android.Manifest
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.EnvironmentCompat
import androidx.core.os.LocaleListCompat
import com.androidtech.data.logger.DataLogger
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm"
const val DATE_TIME_FORMAT_DD_MM_YYY = "dd/MM/yyyy"
const val DATE_TIME_FORMAT_HOUR_MINUTE = "hh:mm a"

val photoContentUri: Uri
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

val videoContentUri: Uri
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

val fileContentUri: Uri
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else
        MediaStore.Files.getContentUri("external")

fun Context.isDebug() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

fun Context.getCountryCode(): String {
    val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val countryCode = telephonyManager.run {
        when {
            simCountryIso != null && simCountryIso.length == 2 -> {
                simCountryIso
            }
            phoneType == TelephonyManager.PHONE_TYPE_CDMA -> {
                getCDMACountryIso()
            }
            else -> {
                networkCountryIso
            }
        }
    } ?: ""

    return if (countryCode.length == 2) {
        countryCode
    } else {
        try {
            resources.configuration.locales[0].country
        } catch (e: Exception) {
            ""
        }
    }.lowercase()

}

fun getCDMACountryIso(): String? {
    try {
        // ISO [MCC] = Mobile country code
        val systemProperties = Class.forName("android.os.SystemProperties")
        val get = systemProperties.getMethod("get", String::class.java)

        // Get homeOperator that contain MCC + MNC
        val homeOperator =
            get.invoke(systemProperties, "ro.cdma.home.operator.numeric") as String?

        // First three characters (MCC) from homeOperator represents the country code
        val mcc = homeOperator?.let {
            try {
                it.substring(0, 3).toInt()
            } catch (e: Exception) {
                0
            }
        }

        val code = when (mcc) {
            330 -> {
                "PR"
            }

            310 -> {
                "US"
            }

            311 -> {
                "US"
            }

            312 -> {
                "US"
            }

            316 -> {
                "US"
            }

            283 -> {
                "AM"
            }

            460 -> {
                "CN"
            }

            455 -> {
                "MO"
            }

            414 -> {
                "MM"
            }

            619 -> {
                "SL"
            }

            450 -> {
                "KR"
            }

            634 -> {
                "SD"
            }

            434 -> {
                "UZ"
            }

            232 -> {
                "AT"
            }

            204 -> {
                "NL"
            }

            262 -> {
                "DE"
            }

            247 -> {
                "LV"
            }

            255 -> {
                "UA"
            }

            else -> null
        }

        return code
    } catch (e: Exception) {
        return null
    }
}

fun Context.resolveContentSizeBytes(contentUri: Uri, reportedSizeBytes: Long): Long {
    if (reportedSizeBytes > 0L) return reportedSizeBytes

    val fromColumns = runCatching {
        contentResolver.query(
            contentUri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (idx < 0) return@use null
            if (!cursor.moveToFirst()) return@use null
            cursor.getLong(idx).takeIf { it > 0L }
        }
    }.getOrNull()
    if (fromColumns != null) return fromColumns

    val fromFd = runCatching {
        contentResolver.openAssetFileDescriptor(contentUri, "r")?.use { afd ->
            afd.length.takeIf { it > 0L }
        }
    }.getOrNull()
    return fromFd ?: reportedSizeBytes
}

fun Long.toFormattedDateTime(
    pattern: String = DATE_TIME_FORMAT,
    locale: Locale = Locale.getDefault()
): String {
    return try {
        val dateFormat = SimpleDateFormat(pattern, locale)
        dateFormat.format(this)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        "Invalid Date"
    }
}

fun Context.hasStoragePermission(): Boolean {
    val permissions = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.resolveVideoDurationMs(contentUri: Uri, reportedDurationMs: Long): Long {
    if (reportedDurationMs > 0L) return reportedDurationMs

    val fromColumns = runCatching {
        contentResolver.query(
            contentUri,
            arrayOf(MediaStore.Video.VideoColumns.DURATION),
            null,
            null,
            null
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
            if (idx < 0) return@use null
            if (!cursor.moveToFirst()) return@use null
            cursor.getLong(idx).takeIf { it > 0L }
        }
    }.getOrNull()
    if (fromColumns != null) return fromColumns

    val fromRetriever = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, contentUri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.takeIf { it > 0L }
        } finally {
            retriever.release()
        }
    }.getOrNull()

    return fromRetriever ?: reportedDurationMs
}

fun Context.getExternalStorageDirectories(): MutableList<String> {
    val externalFileDirs = getExternalFilesDirs(null).filterNotNull()
    var splits: List<String>
    var isExternalStorageRemovable: Boolean
    val externalStorageDirectories = mutableListOf<String>()
    val buildVersionSdk = Build.VERSION.SDK_INT
    if (buildVersionSdk >= 19) {
        for (file in externalFileDirs) {
            splits = file.path.split("/Android")
            if (splits.isNotEmpty()) {
                isExternalStorageRemovable = if (buildVersionSdk >= 21) {
                    Environment.isExternalStorageRemovable(file)
                } else {
                    Environment.MEDIA_MOUNTED == EnvironmentCompat.getStorageState(file)
                }
                if (isExternalStorageRemovable) {
                    externalStorageDirectories.add(splits[0])
                }
            }
        }
    }
    if (externalStorageDirectories.isEmpty()) {
        var byteStr = ""

        try {
            val process = ProcessBuilder(*arrayOfNulls(0))
                .command(*arrayOf("mount | grep /dev/block/vold"))
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            val inputStream = process.inputStream
            val byteArr = ByteArray(1024)
            while (inputStream.read(byteArr) != -1) {
                byteStr += String(byteArr)
            }
            inputStream.close()
        } catch (e: IOException) {

        }
        if (byteStr.trim().isNotBlank()) {
            val byteStrArr = byteStr.split("\n")
            for (byteStrSub in byteStrArr) {
                externalStorageDirectories.add(byteStrSub.split(" ")[2])
            }
        }
    }
    return externalStorageDirectories
}

fun Context.findAllScreenshotFiles(): List<File> {
    val folders = mutableSetOf<File>()

    val internalRoot = Environment.getExternalStorageDirectory().absolutePath
    val candidatePaths = listOf(
        // Stock Android / Pixel / OnePlus / Oppo / Vivo
        "$internalRoot/Pictures/Screenshots",
        "$internalRoot/Pictures/Screenshot",
        // Samsung / Xiaomi / Huawei
        "$internalRoot/DCIM/Screenshots",
        "$internalRoot/DCIM/Screenshot",
        "$internalRoot/Screenshots",
        "$internalRoot/Screenshot",
    )
    candidatePaths
        .map { File(it) }
        .filter { it.exists() && it.isDirectory }
        .forEach { folders.add(it) }

    if (folders.isEmpty()) {
        DataLogger.d("findAllScreenshotFolders: no standard path found, fallback to recursive scan")
        File(internalRoot)
            .walkTopDown()
            .maxDepth(5)
            .filter { file ->
                file.isDirectory && file.name.lowercase().let {
                    it == "screenshots" || it == "screenshot"
                }
            }
            .forEach { folders.add(it) }
    }

    // 3. Scan thêm thẻ SD nếu có
    getExternalStorageDirectories().forEach { sdRoot ->
        val sdCandidates = listOf(
            "${sdRoot}/Pictures/Screenshots",
            "${sdRoot}/Pictures/Screenshot",
            "${sdRoot}/DCIM/Screenshots",
            "${sdRoot}/DCIM/Screenshot",
            "${sdRoot}/Screenshots",
            "${sdRoot}/Screenshot",
        )
        sdCandidates
            .map { File(it) }
            .filter { it.exists() && it.isDirectory }
            .forEach { folders.add(it) }
    }

    DataLogger.d("findAllScreenshotFolders: ${folders.size} folders found -> $folders")
    return folders.toList()
}

fun Context.queryMediaStoreId(absolutePath: String): Long {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val (selection, selectionArgs) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val internalRoot = Environment.getExternalStorageDirectory().absolutePath
        val relativePath = absolutePath
            .removePrefix(internalRoot)
            .trimStart('/')
            .substringBeforeLast('/')
            .let { if (it.endsWith("/")) it else "$it/" }
        val displayName = absolutePath.substringAfterLast('/')

        ("${MediaStore.Images.Media.RELATIVE_PATH} = ? AND " +
                "${MediaStore.Images.Media.DISPLAY_NAME} = ?") to
                arrayOf(relativePath, displayName)
    } else {
        "${MediaStore.Images.Media.DATA} = ?" to arrayOf(absolutePath)
    }

    return contentResolver.query(
        collection,
        arrayOf(MediaStore.Images.Media._ID),
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst())
            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
        else -1L
    } ?: -1L
}

fun Context.getCurrentLanguageCode(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
    } else {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}

fun getDeviceLanguageCodes(): List<String> {
    val deviceLanguageCodes = mutableListOf<String>()
    val localeList = LocaleListCompat.getAdjustedDefault()
    for (i in 0 until localeList.size()) {
        val locale = localeList[i] ?: continue
        val systemLanguageCode = locale.language.lowercase()
        deviceLanguageCodes.add(systemLanguageCode)
    }
    return deviceLanguageCodes
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}