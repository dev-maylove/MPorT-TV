package com.mport.tv.data.config

data class RemoteConfigPayload(val latestVersionCode:Int=1,val latestVersionName:String="1.0.0",val updateUrl:String?=null,val updateSha256:String?=null,val forceUpdate:Boolean=false,val playlistUrl:String?=null,val epgUrl:String?=null,val maintenanceMode:Boolean=false,val message:String?=null)
