package com.homelab.app.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every translated locale must define exactly the keys of the default (English) resources,
 * with non-empty values and the same format placeholders, so no screen falls back to English.
 */
class StringResourcesParityTest {

    private val resDir = File("src/main/res")
    private val locales = listOf("fr", "es", "de", "it")
    private val placeholder = Regex("""%\d+\$[sd]|%[sd]""")

    private fun load(dir: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir, "$dir/strings.xml"))
        val nodes = document.documentElement.childNodes
        return buildMap {
            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as? Element ?: continue
                if (node.getAttribute("translatable") == "false") continue
                put("${node.tagName}:${node.getAttribute("name")}", node.textContent.trim())
            }
        }
    }

    private fun placeholders(text: String): List<String> = placeholder.findAll(text).map { it.value }.sorted().toList()

    @Test
    fun `every locale defines exactly the default keys`() {
        val base = load("values").keys
        locales.forEach { locale ->
            val keys = load("values-$locale").keys
            assertEquals("Missing keys in $locale", emptySet<String>(), base - keys)
            assertEquals("Unknown keys in $locale", emptySet<String>(), keys - base)
        }
    }

    @Test
    fun `no translated value is empty`() {
        locales.forEach { locale ->
            val empty = load("values-$locale").filterValues { it.isEmpty() }.keys
            assertTrue("Empty values in $locale: $empty", empty.isEmpty())
        }
    }

    @Test
    fun `translations keep the default placeholders`() {
        val base = load("values").filterKeys { it.startsWith("string:") }
        locales.forEach { locale ->
            val translated = load("values-$locale")
            val broken = base.filter { (key, text) ->
                translated[key]?.let { placeholders(it) != placeholders(text) } ?: false
            }.keys
            assertTrue("Placeholder mismatch in $locale: $broken", broken.isEmpty())
        }
    }
}
