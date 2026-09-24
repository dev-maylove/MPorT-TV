package com.mport.tv.data.health
import java.net.HttpURLConnection
import java.net.URL
class NetworkHealth { fun checkHttps(url:String):Boolean=runCatching{ val c=URL(url).openConnection() as HttpURLConnection;c.connectTimeout=5000;c.readTimeout=5000;c.requestMethod="HEAD";try{c.responseCode in 200..399}finally{c.disconnect()} }.getOrDefault(false) }
