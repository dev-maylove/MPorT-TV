package com.mport.tv.data.update
import java.io.File
import java.security.MessageDigest
object VerifiedUpdate {
    fun sha256(file:File):String { val d=MessageDigest.getInstance("SHA-256"); file.inputStream().use { input -> val b=ByteArray(65536); while(true){ val n=input.read(b); if(n<0) break; d.update(b,0,n) } }; return d.digest().joinToString(""){ "%02x".format(it) } }
    fun matchesSha256(file:File, expected:String)=sha256(file).equals(expected.trim(),true)
}
