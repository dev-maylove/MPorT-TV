package com.mport.tv.data.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
class UpdateRepository {
    suspend fun download(url:String,destination:File):File=withContext(Dispatchers.IO){ val c=URL(url).openConnection() as HttpURLConnection; c.connectTimeout=15000;c.readTimeout=60000;c.instanceFollowRedirects=true; try{c.inputStream.use{input->destination.outputStream().use{out->input.copyTo(out)}};destination}finally{c.disconnect()} }
}
