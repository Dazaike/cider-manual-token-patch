package com.dazaike.ciderpatcher.core

import com.android.apksig.ApkVerifier
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

private fun ZipFile.dex(name: String): DexBackedDexFile =
    DexBackedDexFile(com.android.tools.smali.dexlib2.Opcodes.getDefault(), getInputStream(getEntry(name)).use { it.readBytes() })

private fun DexBackedDexFile.methodNames(type: String): Set<String> =
    classes.single { it.type == type }.methods.map { it.name }.toSet()

class PipelineE2ETest {
    @Test
    fun patchesV129EndToEnd() {
        val input = TestFixtures.apk("CIDER_APK_129")
        val out = File("build/e2e/cider-129-patched.apk").apply { parentFile.mkdirs(); delete() }
        val key = SigningKey.generate()
        val log = mutableListOf<String>()
        CiderPatcher.patch(input, out, key, log = { log += it })
        assertEquals("Done", log.last())
        assertTrue(log.toString(), log.contains(CiderPatcher.SID_LOG_APPLIED))

        ZipFile(out).use { zip ->
            val front = zip.dex("classes.dex")
            assertTrue(front.methodNames("Lc/kq7;").containsAll(listOf("l", "l\$cloudOrig", "s", "s\$cloudOrig")))
            assertTrue(front.methodNames("Lc/l0c;").containsAll(listOf("k", "k\$cloudOrig", "h", "h\$cloudOrig")))
            assertTrue(front.methodNames("Lc/tzb;").containsAll(listOf("a", "a\$cloudOrig")))
            assertTrue(front.classes.any { it.type == "Lcom/cidercollective/cider/sidcloud/SidGlue;" })

            ZipFile(input).use { src ->
                assertArrayEquals(
                    src.getInputStream(src.getEntry("classes.dex")).use { it.readBytes() },
                    zip.getInputStream(zip.getEntry("classes2.dex")).use { it.readBytes() },
                )
            }
            assertTrue(zip.dex("classes3.dex").classes.any { it.type == "Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity;" })
            assertTrue(zip.dex("classes4.dex").classes.any { it.type == "Lcom/cidercollective/cider/sidcloud/CloudSid;" })
        }

        assertTrue(ApkVerifier.Builder(out).build().verify().isVerified)

        try {
            CiderPatcher.patch(out, File("build/e2e/twice-129.apk"), key)
            fail("patched APK was accepted again")
        } catch (e: PatchException) {
            assertEquals("This APK is already patched", e.message)
        }
    }

    @Test
    fun patchesV93EndToEnd() {
        val input = TestFixtures.apk("CIDER_APK_93")
        val out = File("build/e2e/cider-93-patched.apk").apply { parentFile.mkdirs(); delete() }
        val key = SigningKey.generate()
        val log = mutableListOf<String>()
        CiderPatcher.patch(input, out, key, log = { log += it })
        assertEquals("Done", log.last())
        assertTrue(!File(out.path + ".unsigned").exists())

        ZipFile(out).use { zip ->
            assertNotNull(zip.getEntry("classes2.dex"))
            // No Sid in 1.0.93: layout is original, token patch, cloud client.
            assertTrue(zip.dex("classes2.dex").classes.any { it.type == "Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity;" })
            assertTrue(zip.dex("classes3.dex").classes.any { it.type == "Lcom/cidercollective/cider/sidcloud/CloudSid;" })
            val manifest = AndroidManifestBlock.load(zip.getInputStream(zip.getEntry("AndroidManifest.xml")))
            val activity = manifest.getActivity(CiderPatcher.PATCH_ACTIVITY, false)
            assertNotNull(activity)
            val attrIds = activity.attributes.asSequence().map { it.nameId }.toList()
            assertEquals(listOf(0x01010000, 0x01010003, 0x01010010, 0x01010017), attrIds)
            val byId = activity.attributes.asSequence().associateBy { it.nameId }
            assertEquals(ManifestPatcher.THEME_DEVICE_DEFAULT_PANEL, byId.getValue(0x01010000).data)
            assertEquals(CiderPatcher.PATCH_ACTIVITY, byId.getValue(0x01010003).valueAsString)
            assertTrue(byId.getValue(0x01010010).valueAsBoolean)
            assertTrue(byId.getValue(0x01010017).valueAsBoolean)
            assertEquals(0, activity.elementsCount)
        }

        assertTrue(ApkVerifier.Builder(out).build().verify().isVerified)

        try {
            CiderPatcher.patch(out, File("build/e2e/twice.apk"), key)
            fail("patched APK was accepted again")
        } catch (e: PatchException) {
            assertEquals("This APK is already patched", e.message)
        }
    }
}
