package com.dazaike.ciderpatcher.core

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

/** Verifies every app symbol the assembled patch touches exists in the target build. */
object LinkChecker {
    private val PLATFORM_PREFIXES = listOf("Landroid/", "Ljava/", "Ldalvik/")

    fun check(newDex: DexBackedDexFile, app: DexIndex) {
        for (cls in newDex.classes) {
            checkType(cls.superclass ?: "Ljava/lang/Object;", app)
            cls.interfaces.forEach { checkType(it, app) }
            for (method in cls.methods) {
                method.parameterTypes.forEach { checkType(it.toString(), app) }
                checkType(method.returnType, app)
                val insns = method.implementation?.instructions ?: continue
                for (insn in insns) {
                    when (val ref = (insn as? ReferenceInstruction)?.reference) {
                        is FieldReference -> checkField(ref, app)
                        is MethodReference -> checkMethod(ref, app)
                        is TypeReference -> checkType(ref.type, app)
                    }
                }
            }
            checkAbstractsImplemented(cls, app)
        }
    }

    private fun missing(ref: String): Nothing = throw PatchException("Patch references missing symbol $ref")

    private fun checkType(rawType: String, app: DexIndex) {
        val type = rawType.trimStart('[')
        if (!type.startsWith("L") || type.startsWith(PATCH_PACKAGE_PREFIX)) return
        if (type in app.classes) return
        if (PLATFORM_PREFIXES.none { type.startsWith(it) }) missing(type)
    }

    private fun checkField(ref: FieldReference, app: DexIndex) {
        checkType(ref.type, app)
        val owner = ref.definingClass
        if (owner.startsWith(PATCH_PACKAGE_PREFIX)) return
        if (owner !in app.classes) return checkType(owner, app)
        val found = hierarchy(owner, app).any { c -> c.fields.any { it.name == ref.name && it.type == ref.type } }
        if (!found) missing(fieldDescriptor(ref))
    }

    private fun checkMethod(ref: MethodReference, app: DexIndex) {
        ref.parameterTypes.forEach { checkType(it.toString(), app) }
        checkType(ref.returnType, app)
        val owner = ref.definingClass
        if (owner.startsWith(PATCH_PACKAGE_PREFIX)) return
        if (owner !in app.classes) return checkType(owner, app)
        val key = signature(ref)
        val found = hierarchy(owner, app).any { c -> c.methods.any { signature(it) == key } }
        if (!found) missing(methodDescriptor(ref))
    }

    /** Every abstract method inherited from app classes must have a concrete implementation in the new class or its app superclasses. */
    private fun checkAbstractsImplemented(cls: ClassDef, app: DexIndex) {
        val chain = listOf(cls) + generateSequence(cls.superclass?.let { app.classes[it] }) { c ->
            c.superclass?.let { app.classes[it] }
        }
        val concrete = chain.flatMap { c -> c.methods.filter { !AccessFlags.ABSTRACT.isSet(it.accessFlags) } }
            .map { signature(it) }.toSet()
        val roots = listOfNotNull(cls.superclass) + cls.interfaces
        for (c in roots.flatMap { hierarchy(it, app) }.distinctBy { it.type }) {
            for (m in c.methods) {
                if (AccessFlags.ABSTRACT.isSet(m.accessFlags) && signature(m) !in concrete) missing(methodDescriptor(m))
            }
        }
    }

    /** [type] plus its superclasses and interfaces that exist in the app index. */
    private fun hierarchy(type: String, app: DexIndex): List<ClassDef> {
        val seen = LinkedHashMap<String, ClassDef>()
        val queue = ArrayDeque(listOf(type))
        while (queue.isNotEmpty()) {
            val t = queue.removeFirst()
            if (t in seen) continue
            val c = app.classes[t] ?: continue
            seen[t] = c
            c.superclass?.let { queue += it }
            queue += c.interfaces
        }
        return seen.values.toList()
    }

    private fun signature(m: MethodReference): String =
        m.name + m.parameterTypes.joinToString("", "(", ")") + m.returnType
}
