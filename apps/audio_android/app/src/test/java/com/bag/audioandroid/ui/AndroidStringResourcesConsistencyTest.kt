package com.bag.audioandroid.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidStringResourcesConsistencyTest {
    @Test
    fun `all maintained locale string files contain same keys as base values`() {
        val resourceMaps = loadMaintainedLocaleStringMaps()
        val baseKeys = resourceMaps.getValue(BaseLocaleDirectory).keys

        resourceMaps.forEach { (directory, strings) ->
            assertEquals(
                "String key set mismatch for $directory",
                baseKeys,
                strings.keys,
            )
        }
    }

    @Test
    fun `all maintained locale string files keep placeholder signatures aligned with base values`() {
        val resourceMaps = loadMaintainedLocaleStringMaps()
        val baseStrings = resourceMaps.getValue(BaseLocaleDirectory)

        MaintainedLocaleDirectories
            .filter { it != BaseLocaleDirectory }
            .forEach { directory ->
                val localizedStrings = resourceMaps.getValue(directory)
                baseStrings.forEach { (key, baseValue) ->
                    val localizedValue = localizedStrings.getValue(key)
                    assertEquals(
                        "Placeholder signature mismatch for key '$key' in $directory",
                        placeholderSignature(baseValue),
                        placeholderSignature(localizedValue),
                    )
                }
            }
    }

    @Test
    fun `all maintained locale string files keep values non blank`() {
        loadMaintainedLocaleStringMaps().forEach { (directory, strings) ->
            strings.forEach { (key, value) ->
                assertTrue(
                    "Blank string value for key '$key' in $directory",
                    value.isNotBlank(),
                )
            }
        }
    }

    private fun loadMaintainedLocaleStringMaps(): Map<String, Map<String, String>> =
        MaintainedLocaleDirectories.associateWith { directory ->
            parseStringsXml(resolveStringsFile(directory))
        }

    private fun resolveStringsFile(directory: String): File {
        val file = File("src/main/res/$directory/strings.xml")
        require(file.exists()) { "Missing strings.xml for $directory at ${file.absolutePath}" }
        return file
    }

    private fun parseStringsXml(file: File): Map<String, String> {
        val documentBuilder =
            DocumentBuilderFactory
                .newInstance()
                .apply {
                    isNamespaceAware = false
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                }.newDocumentBuilder()
        val document = documentBuilder.parse(file)
        val nodes = document.getElementsByTagName("string")
        val values = linkedMapOf<String, String>()

        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            val attributes = node.attributes ?: continue
            val key = attributes.getNamedItem("name")?.nodeValue ?: continue
            values[key] = node.textContent.orEmpty().trim()
        }
        return values
    }

    private fun placeholderSignature(value: String): List<String> {
        val placeholders = mutableListOf<String>()
        var index = 0
        while (index < value.length) {
            if (value[index] != '%') {
                index += 1
                continue
            }
            if (index + 1 < value.length && value[index + 1] == '%') {
                index += 2
                continue
            }
            val match = PlaceholderRegex.find(value, index)
            if (match != null && match.range.first == index) {
                val position = match.groups[1]?.value.orEmpty()
                val conversion = match.groups[2]?.value.orEmpty()
                placeholders += "$position$conversion"
                index = match.range.last + 1
            } else {
                index += 1
            }
        }
        return placeholders
    }

    companion object {
        private const val BaseLocaleDirectory = "values"

        private val MaintainedLocaleDirectories =
            listOf(
                "values",
                "values-zh",
                "values-zh-rTW",
                "values-ja",
                "values-de",
                "values-es",
                "values-pt-rBR",
                "values-ru",
                "values-uk",
            )

        private val PlaceholderRegex =
            Regex("%(?:(\\d+)\\$)?(?:[-#+ 0,(<]*)?(?:\\d+)?(?:\\.\\d+)?(?:[tT])?([a-zA-Z])")
    }
}
