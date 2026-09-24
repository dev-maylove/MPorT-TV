package com.mport.tv.data.player

data class DrmConfig(val scheme: DrmScheme, val licenseUrl: String? = null, val requestHeaders: Map<String,String> = emptyMap())
enum class DrmScheme { NONE, WIDEVINE, CLEAR_KEY }
