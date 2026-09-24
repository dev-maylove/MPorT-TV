package com.mport.tv.data.config

data class AppConfig(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val updateUrl: String? = null,
    val updateForce: Boolean = false,
    val updateReleaseNotes: String = "",
    val playlistUrl: String? = null,
    val epgUrl: String? = null,
    val maintenanceMode: Boolean = false
)
