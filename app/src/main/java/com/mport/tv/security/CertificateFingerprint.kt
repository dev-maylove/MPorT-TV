package com.mport.tv.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object CertificateFingerprint {

    fun sha256(context: Context): String {
        val pm = context.packageManager
        val packageName = context.packageName
        val cert = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = info.signingInfo
                ?: throw IllegalStateException("signingInfo is null")
            val signers = signingInfo.apkContentsSigners
            if (signers.isEmpty()) throw IllegalStateException("No signing certificates")
            signers[0]
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            val signatures = info.signatures
                ?: throw IllegalStateException("signatures is null")
            if (signatures.isEmpty()) throw IllegalStateException("No signatures")
            signatures[0]
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(cert.toByteArray())
        return digest.joinToString(":") { byte -> "%02X".format(byte) }
    }
}
