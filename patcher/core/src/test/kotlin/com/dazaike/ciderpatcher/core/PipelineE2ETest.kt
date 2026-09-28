package com.dazaike.ciderpatcher.core

import com.android.apksig.ApkVerifier
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class PipelineE2ETest {
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
