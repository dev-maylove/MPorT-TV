package com.mport.tv.data.telemetry
interface Telemetry { fun event(name:String,attributes:Map<String,String> = emptyMap()); fun error(error:Throwable,attributes:Map<String,String> = emptyMap()) }
class NoOpTelemetry:Telemetry { override fun event(name:String,attributes:Map<String,String>)=Unit; override fun error(error:Throwable,attributes:Map<String,String>)=Unit }
