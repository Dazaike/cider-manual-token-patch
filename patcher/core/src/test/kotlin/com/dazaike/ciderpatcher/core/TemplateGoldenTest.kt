package com.dazaike.ciderpatcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TemplateGoldenTest {
    private fun golden(file: String) = File(TestFixtures.GOLDEN_DIR, file).readText().replace("\r\n", "\n")

    /** Splits out every `# BEGIN name` … `# END name` block (plus the blank line after it). Returns (remaining text, name → block lines). */
    private fun stripPatcherBlocks(text: String): Pair<String, Map<String, List<String>>> {
        val lines = text.split("\n")
        val kept = mutableListOf<String>()
        val blocks = mutableMapOf<String, MutableList<String>>()
        var i = 0
        while (i < lines.size) {
            val begin = Regex("""^\s*# BEGIN (\S+)$""").matchEntire(lines[i])
            if (begin == null) {
                kept += lines[i++]
                continue
            }
            val name = begin.groupValues[1]
            val block = blocks.getOrPut(name) { mutableListOf() }
            while (!lines[i].trim().startsWith("# END $name")) block += lines[i++]
            block += lines[i++]
            assertEquals("blank line after END $name", "", lines[i])
            i++
        }
        return kept.joinToString("\n") to blocks
    }

    @Test
    fun v93RenderingWithoutPatcherBlocksReproducesGoldenPatch() {
        val rendered = TemplateRenderer.render(
            SymbolMap.withDerived(TestFixtures.V93, CreateOrder.CONT_FIRST)
        )
        assertEquals(10, rendered.size)
        val blocksByFile = mutableMapOf<String, Map<String, List<String>>>()
        for ((file, text) in rendered) {
            val (stripped, blocks) = stripPatcherBlocks(text.replace("\r\n", "\n"))
            assertEquals(file, golden(file), stripped)
            blocksByFile[file] = blocks
        }
        val tokenExtra = blocksByFile.getValue("ManualAppleTokenActivity.smali").getValue("token-extra")
        assertTrue(tokenExtra.contains("    const-string v2, \"${CiderPatcher.TOKEN_EXTRA}\""))
        val result = blocksByFile.getValue("SignInCoroutine.smali").getValue("sign-in-result")
        assertTrue(result.contains("    const-string v6, \"${CiderPatcher.RESULT_EXTRA}\""))
        assertTrue(result.contains("    const-string v4, \"${CiderPatcher.RESULT_INVALID}\""))
        assertTrue(result.contains("    const-string v4, \"${CiderPatcher.RESULT_UNVERIFIED}\""))
        assertTrue("sign-in-result" in blocksByFile.getValue("SignInCoroutine\$FinishRunnable.smali"))
        assertTrue("sign-in-result" in blocksByFile.getValue("SignInCoroutine\$InvalidTokenRunnable.smali"))
    }

    @Test
    fun templatesContainNoHardcodedObfuscatedNames() {
        for (file in TemplateRenderer.TEMPLATE_FILES) {
            assertFalse(file, TemplateRenderer.loadTemplate(file).contains("Lc/"))
        }
    }

    @Test(expected = PatchException::class)
    fun missingKeyFails() {
        TemplateRenderer.render(SymbolMap.withDerived(TestFixtures.V93 - "M_ENCRYPT", CreateOrder.CONT_FIRST))
    }
}
