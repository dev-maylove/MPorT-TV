package com.mport.tv.data.stalker

data class PortalConfig(val baseUrl: String, val macAddress: String? = null, val token: String? = null)
data class PortalChannel(val id: String, val name: String, val streamUrl: String, val logoUrl: String? = null, val groupId: String? = null, val epgId: String? = null)
data class PortalGroup(val id: String, val name: String)
