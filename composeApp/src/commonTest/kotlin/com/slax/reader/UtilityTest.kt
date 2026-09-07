package com.slax.reader

import com.slax.reader.utils.MarkdownHelper
import com.slax.reader.utils.StreamMarkdownProcessor
import com.slax.reader.utils.escapeJsTemplateString
import com.slax.reader.utils.getMimeTypeFromUrl
import com.slax.reader.domain.image.normalizeImageUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UtilityTest {
    @Test
    fun markdown_links_encode_special_url_characters_without_double_encoding() {
        assertEquals(
            "[Read](https://example.com/a%20b%28c%29)",
            MarkdownHelper.fixMarkdownLinks("[Read](https://example.com/a b(c))")
        )
        assertEquals(
            "[Read](https://example.com/a%20b%28c%29)",
            MarkdownHelper.fixMarkdownLinks("[Read](https://example.com/a%20b%28c%29)")
        )
        assertEquals("plain text", MarkdownHelper.fixMarkdownLinks("plain text"))
    }

    @Test
    fun stream_markdown_processor_keeps_incomplete_links_until_flush() {
        val processor = StreamMarkdownProcessor()
        assertEquals("Read ", processor.process("Read ["))
        assertEquals("Read ", processor.process("Read [Title]("))
        assertEquals(
            "Read [Title](https://example.com/a%20b)",
            processor.process("Read [Title](https://example.com/a b)").let { processor.flush() }
        )
    }

    @Test
    fun mime_type_helper_handles_query_strings_and_unknown_extensions() {
        assertEquals("image/png", getMimeTypeFromUrl("https://x.test/a.PNG?size=2"))
        assertEquals("image/svg+xml", getMimeTypeFromUrl("https://x.test/icon.svg"))
        assertEquals("image/jpeg", getMimeTypeFromUrl("https://x.test/no-extension"))
    }

    @Test
    fun javascript_template_escaping_handles_control_characters() {
        assertEquals("\\\\\\`\\$\\n\\r\\t", escapeJsTemplateString("\\`$\n\r\t"))
    }

    @Test
    fun malformed_selection_json_returns_null() {
        assertNull(com.slax.reader.utils.parseSelectionData("{not-json}"))
    }

    @Test
    fun image_cache_urls_are_normalized_before_lookup() {
        assertEquals("https://example.com/a.png", normalizeImageUrl("slaxstatics://example.com/a.png"))
        assertEquals("http://example.com/a.png", normalizeImageUrl("slaxstatic://example.com/a.png"))
        assertEquals("https://example.com/a.png", normalizeImageUrl("https://example.com/a.png"))
    }
}
