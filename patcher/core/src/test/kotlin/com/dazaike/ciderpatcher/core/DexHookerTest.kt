package com.dazaike.ciderpatcher.core

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexHookerTest {
    private fun dex(vararg files: Pair<String, String>) = DexAssembler.assemble(mapOf(*files))

    private fun parse(bytes: ByteArray) = DexBackedDexFile(Opcodes.getDefault(), bytes)

    private val target = """
        .class public final Lt/Target;
        .super Ljava/lang/Object;

        .method public final foo(I)I
            .locals 1
            const/4 v0, 0x0
            return v0
        .end method

        .method public final bar()Ljava/lang/String;
            .locals 1
            const-string v0, "keep me"
            return-object v0
        .end method
    """.trimIndent()

    private val stub = """
        .class public final Lt/Target;
        .super Ljava/lang/Object;

        .method public static foo(I)I
            .locals 1
            const/4 v0, 0x1
            return v0
        .end method
    """.trimIndent()

    private fun constOf(cls: com.android.tools.smali.dexlib2.iface.ClassDef, name: String): Long {
        val m = cls.methods.single { it.name == name }
        return (m.implementation!!.instructions.first() as WideLiteralInstruction).wideLiteral
    }

    @Test
    fun hookRenamesOriginalAndInstallsWrapperWithOriginalFlags() {
        val index = DexIndex(listOf(parse(dex("Target.smali" to target))))
        val stubs = parse(dex("Target.smali" to stub))
        val out = DexHooker.build(index, listOf(DexHooker.Hook("Lt/Target;", "foo", listOf("I"), "I")), stubs, null)

        val cls = parse(out).classes.single { it.type == "Lt/Target;" }
        assertEquals(setOf("foo", "foo${DexHooker.ORIG_SUFFIX}", "bar"), cls.methods.map { it.name }.toSet())
        assertEquals(1L, constOf(cls, "foo"))
        assertEquals(0L, constOf(cls, "foo${DexHooker.ORIG_SUFFIX}"))
        val flags = AccessFlags.PUBLIC.value or AccessFlags.FINAL.value
        assertEquals(flags, cls.methods.single { it.name == "foo" }.accessFlags)
        assertEquals(flags, cls.methods.single { it.name == "foo${DexHooker.ORIG_SUFFIX}" }.accessFlags)
    }

    @Test
    fun footnoteReplacesOnlyTheExactString() {
        val other = """
            .class public final Lt/Other;
            .super Ljava/lang/Object;

            .method public final text()Ljava/lang/String;
                .locals 1
                const-string v0, "old text"
                return-object v0
            .end method

            .method public final other()Ljava/lang/String;
                .locals 1
                const-string v0, "old text and more"
                return-object v0
            .end method
        """.trimIndent()
        val index = DexIndex(listOf(parse(dex("Target.smali" to target, "Other.smali" to other))))
        val stubs = parse(dex("Target.smali" to stub))
        val out = DexHooker.build(index, listOf(DexHooker.Hook("Lt/Target;", "foo", listOf("I"), "I")), stubs, "old text" to "new text")

        val classes = parse(out).classes.associateBy { it.type }
        fun strings(type: String, method: String) = classes.getValue(type).methods.single { it.name == method }
            .implementation!!.instructions.mapNotNull {
                ((it as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)?.reference
                    as? com.android.tools.smali.dexlib2.iface.reference.StringReference)?.string
            }
        assertEquals(listOf("new text"), strings("Lt/Other;", "text"))
        assertEquals(listOf("old text and more"), strings("Lt/Other;", "other"))
        assertEquals(listOf("keep me"), strings("Lt/Target;", "bar"))
        assertNotNull(classes["Lt/Target;"])
        assertTrue(classes.size == 2)
    }

    @Test(expected = PatchException::class)
    fun missingTargetMethodFails() {
        val index = DexIndex(listOf(parse(dex("Target.smali" to target))))
        val stubs = parse(dex("Target.smali" to stub))
        DexHooker.build(index, listOf(DexHooker.Hook("Lt/Target;", "nope", listOf("I"), "I")), stubs, null)
    }
}
