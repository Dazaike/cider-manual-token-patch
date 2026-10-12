package com.dazaike.ciderpatcher.core

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.ImmutableClassDef
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool

/**
 * Builds a dex holding modified copies of existing app classes. For each [Hook] the original method is
 * renamed `name$cloudOrig` (same flags, same body) and a wrapper with the original name, taken from the
 * stub dex, takes its place. The result is meant to be the FIRST dex of the APK so these definitions
 * shadow the originals at class load.
 */
object DexHooker {
    const val ORIG_SUFFIX = "\$cloudOrig"

    data class Hook(val type: String, val name: String, val params: List<String>, val returnType: String)

    /**
     * [stubs] holds one class per hooked type (containing only the wrapper methods) plus any extra classes,
     * which are copied unchanged. [footnote] (old, new) rewrites an exact const-string wherever it occurs; no
     * occurrence is fine.
     */
    fun build(index: DexIndex, hooks: List<Hook>, stubs: DexBackedDexFile, footnote: Pair<String, String>?): ByteArray {
        val stubClasses = stubs.classes.associateBy { it.type }
        val hookedTypes = hooks.map { it.type }.distinct()
        val result = LinkedHashMap<String, ClassDef>()
        for (type in hookedTypes) {
            val original = index.classes[type] ?: throw PatchException("Hook target class not found: $type")
            val stub = stubClasses[type] ?: throw PatchException("Missing hook stub class $type")
            result[type] = hookClass(original, stub, hooks.filter { it.type == type })
        }
        for ((type, cls) in stubClasses) if (type !in hookedTypes) result[type] = cls

        if (footnote != null) {
            for (owner in index.methodsWithString(footnote.first).map { it.definingClass }.distinct()) {
                val base = result[owner] ?: index.classes.getValue(owner)
                result[owner] = replaceString(base, footnote.first, footnote.second)
            }
        }

        val pool = DexPool(Opcodes.forApi(33))
        for (cls in result.values) pool.internClass(cls)
        val store = MemoryDataStore()
        pool.writeTo(store)
        return store.data
    }

    private fun matches(m: Method, h: Hook) =
        m.name == h.name && m.returnType == h.returnType && m.parameterTypes.map { it.toString() } == h.params

    private fun hookClass(original: ClassDef, stub: ClassDef, hooks: List<Hook>): ClassDef {
        val methods = ArrayList<Method>()
        val remaining = hooks.toMutableList()
        for (m in original.methods) {
            val hook = remaining.firstOrNull { matches(m, it) }
            if (hook == null) {
                methods += m
                continue
            }
            remaining -= hook
            val wrapper = stub.methods.firstOrNull { matches(it, hook) }
                ?: throw PatchException("Missing hook wrapper ${hook.type}->${hook.name}")
            methods += ImmutableMethod(
                m.definingClass, m.name + ORIG_SUFFIX, m.parameters, m.returnType,
                m.accessFlags, m.annotations, m.hiddenApiRestrictions, m.implementation,
            )
            // The wrapper takes the original's access flags so it stays static/virtual exactly like its callers expect.
            methods += ImmutableMethod(
                wrapper.definingClass, wrapper.name, wrapper.parameters, wrapper.returnType,
                m.accessFlags, wrapper.annotations, wrapper.hiddenApiRestrictions, wrapper.implementation,
            )
        }
        if (remaining.isNotEmpty()) {
            val h = remaining.first()
            throw PatchException("Hook target not found: ${h.type}->${h.name}(${h.params.joinToString("")})${h.returnType}")
        }
        return rebuild(original, methods)
    }

    private fun replaceString(cls: ClassDef, old: String, new: String): ClassDef {
        val methods = cls.methods.map { m ->
            val impl = m.implementation
            if (impl == null || impl.instructions.none { isConstString(it, old) }) return@map m
            val mutable = MutableMethodImplementation(impl)
            for (i in mutable.instructions.indices) {
                val insn = mutable.instructions[i]
                if (!isConstString(insn, old)) continue
                val reg = (insn as OneRegisterInstruction).registerA
                val ref = ImmutableStringReference(new)
                mutable.replaceInstruction(
                    i,
                    if (insn.opcode == Opcode.CONST_STRING) BuilderInstruction21c(Opcode.CONST_STRING, reg, ref)
                    else BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, reg, ref),
                )
            }
            ImmutableMethod(m.definingClass, m.name, m.parameters, m.returnType, m.accessFlags, m.annotations, m.hiddenApiRestrictions, mutable)
        }
        return rebuild(cls, methods)
    }

    private fun isConstString(insn: com.android.tools.smali.dexlib2.iface.instruction.Instruction, s: String) =
        (insn.opcode == Opcode.CONST_STRING || insn.opcode == Opcode.CONST_STRING_JUMBO) &&
            ((insn as ReferenceInstruction).reference as StringReference).string == s

    private fun rebuild(cls: ClassDef, methods: List<Method>) = ImmutableClassDef(
        cls.type, cls.accessFlags, cls.superclass, cls.interfaces, cls.sourceFile,
        cls.annotations, cls.fields, methods,
    )
}
