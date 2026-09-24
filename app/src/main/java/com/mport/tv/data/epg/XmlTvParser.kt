package com.mport.tv.data.epg

import android.util.Xml
import com.mport.tv.core.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

object XmlTvParser {
    fun parse(xml: String): List<EpgProgram> {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        val result = mutableListOf<EpgProgram>()

        var event = parser.eventType
        var inside = false
        var channelId = ""
        var title = ""
        var description: String? = null
        var start = 0L
        var end = 0L

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        inside = true
                        channelId = parser.getAttributeValue(null, "channel") ?: ""
                        start = parseDate(parser.getAttributeValue(null, "start"))
                        end = parseDate(parser.getAttributeValue(null, "stop"))
                        title = ""
                        description = null
                    }
                    "title" -> if (inside) title = parser.nextText()
                    "desc" -> if (inside) description = parser.nextText()
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme" && inside) {
                    result += EpgProgram(
                        "$channelId-$start", channelId, title, description, start, end
                    )
                    inside = false
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseDate(value: String?): Long = runCatching {
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
            .parse(value!!.trim())?.time ?: 0L
    }.getOrDefault(0L)
}
