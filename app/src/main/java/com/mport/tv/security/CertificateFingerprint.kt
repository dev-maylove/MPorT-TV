package com.mport.tv.security
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
object CertificateFingerprint {
    fun sha256(context:Context):String { val pm=context.packageManager; val info=if(Build.VERSION.SDK_INT>=28) pm.getPackageInfo(context.packageName,PackageManager.GET_SIGNING_CERTIFICATES) else @Suppress("DEPRECATION") pm.getPackageInfo(context.packageName,PackageManager.GET_SIGNATURES); val cert=if(Build.VERSION.SDK_INT>=28) info.signingInfo.apkContentsSigners.first() else @Suppress("DEPRECATION") info.signatures.first(); return MessageDigest.getInstance("SHA-256").digest(cert.toByteArray()).joinToString(":"){ "%02X".format(it) } }
}
