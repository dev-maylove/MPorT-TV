package com.mport.tv.security

import android.content.Context
import android.os.Build

class IntegrityManager(private val context: Context) {
    fun versionCode(): Long {
        val p = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong()
    }

    fun versionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}
