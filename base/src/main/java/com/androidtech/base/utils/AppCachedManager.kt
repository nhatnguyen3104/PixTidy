package com.androidtech.base.utils

import android.content.Context
import java.io.File

object AppCacheManager {

    /**
     * Clear all internal and external app cache directories.
     */
    fun clearAppCache(context: Context) {
        runCatching {
            deleteDir(context.cacheDir)
            context.externalCacheDir?.let { deleteDir(it) }
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return false
        val children = dir.listFiles()
        if (children != null) {
            for (child in children) {
                if (child.isDirectory) {
                    deleteDir(child)
                } else {
                    child.delete()
                }
            }
        }
        return dir.delete()
    }
}