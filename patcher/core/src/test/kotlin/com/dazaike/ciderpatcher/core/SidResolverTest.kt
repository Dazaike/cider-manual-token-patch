package com.dazaike.ciderpatcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

private fun resolveSid(apk: File): SidSymbols? = ZipFile(apk).use { zip ->
    val index = DexIndex(DexIndex.parse(DexIndex.readDexEntries(zip)))
    SidResolver(index, SymbolResolver(index).resolve()).resolve()
}

class SidResolverTest {
    @Test
    fun resolvesV129SidNames() {
        val sid = resolveSid(TestFixtures.apk("CIDER_APK_129"))
        assertNotNull(sid)
        for ((key, value) in TestFixtures.V129_SID) assertEquals(key, value, sid!!.values[key])
        assertEquals(TestFixtures.V129_SID.keys + "F_UNIT_INSTANCE", sid!!.values.keys)
        assertEquals(
            listOf("Lc/tzb;.a", "Lc/kq7;.l", "Lc/kq7;.s", "Lc/l0c;.h", "Lc/l0c;.k"),
            sid.hooks.map { "${it.type}.${it.name}" },
        )
    }

    @Test
    fun v93HasNoSid() {
        assertNull(resolveSid(TestFixtures.apk("CIDER_APK_93")))
    }
}
