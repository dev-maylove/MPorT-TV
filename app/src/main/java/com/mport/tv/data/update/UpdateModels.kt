package com.mport.tv.data.update

data class UpdateConfig(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String,
    val forceUpdate: Boolean,
    val releaseNotes: String
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Available : UpdateState
    data class Error(val message: String) : UpdateState
}
