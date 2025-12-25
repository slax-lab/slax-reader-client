package com.slax.reader.utils


object MarkdownHelper {

    fun createStreamProcessor() = StreamMarkdownProcessor()

    fun fixMarkdownLinks(markdown: String): String {
        if (markdown.isEmpty()) return markdown

        val output = StringBuilder(markdown.length)
        var i = 0

        while (i < markdown.length) {
            if (markdown[i] == '[') {
                val result = tryParseLink(markdown, i)
                if (result != null) {
                    output.append('[').append(result.text).append("](")
                        .append(result.url.encodeUrl()).append(')')
                    i = result.endIndex
                    continue
                }
            }
            output.append(markdown[i])
            i++
        }

        return output.toString()
    }

    private fun tryParseLink(text: String, start: Int): LinkParseResult? {
        var i = start + 1

        // 找 ]
        val textEnd = text.indexOf(']', i)
        if (textEnd == -1 || textEnd + 1 >= text.length || text[textEnd + 1] != '(') return null

        // 找匹配的 )
        val urlStart = textEnd + 2
        var depth = 0
        i = urlStart

        while (i < text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> if (depth == 0) {
                    return LinkParseResult(
                        text = text.substring(start + 1, textEnd),
                        url = text.substring(urlStart, i),
                        endIndex = i + 1
                    )
                } else depth--
                '\\' -> i++
            }
            i++
        }

        return null
    }

    private data class LinkParseResult(val text: String, val url: String, val endIndex: Int)

    private fun String.encodeUrl(): String {
        if (contains("%28") || contains("%29")) return this
        return replace("(", "%28").replace(")", "%29")
            .replace("[", "%5B").replace("]", "%5D")
            .replace("{", "%7B").replace("}", "%7D")
            .replace(" ", "%20")
    }
}

class StreamMarkdownProcessor {
    private val output = StringBuilder()
    private val pending = StringBuilder()
    private var lastLen = 0

    fun process(fullContent: String): String {
        if (fullContent.length <= lastLen) return output.toString()

        pending.append(fullContent.substring(lastLen))
        lastLen = fullContent.length

        // 找到最后一个 [ 的位置
        val lastBracket = pending.indexOfLast { it == '[' }

        if (lastBracket == -1) {
            // 没有 [，全部安全
            output.append(MarkdownHelper.fixMarkdownLinks(pending.toString()))
            pending.clear()
        } else if (lastBracket > 0) {
            // 输出 [ 之前的部分
            val safe = pending.substring(0, lastBracket)
            output.append(MarkdownHelper.fixMarkdownLinks(safe))
            // 保留 [ 及之后的内容
            val remaining = pending.substring(lastBracket)
            pending.clear()
            pending.append(remaining)
        }

        return output.toString()
    }

    fun flush(): String {
        if (pending.isNotEmpty()) {
            output.append(MarkdownHelper.fixMarkdownLinks(pending.toString()))
            pending.clear()
        }
        return output.toString()
    }
}
