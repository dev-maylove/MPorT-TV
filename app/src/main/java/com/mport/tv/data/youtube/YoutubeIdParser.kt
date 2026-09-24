package com.mport.tv.data.youtube

object YoutubeIdParser {
    private val patterns = listOf(
        Regex("""(?:youtube\.com/watch\?.*v=|youtube\.com/embed/|youtube\.com/v/|youtu\.be/|youtube\.com/shorts/|youtube\.com/live/)([a-zA-Z0-9_-]{11})"""),
        Regex("""^[a-zA-Z0-9_-]{11}$""")
    )

    fun extract(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        for (p in patterns) {
            val m = p.find(trimmed)
            if (m != null) {
                val id = if (m.groupValues.size > 1) m.groupValues[1] else m.value
                if (id.length == 11) return id
            }
        }
        return null
    }
}
