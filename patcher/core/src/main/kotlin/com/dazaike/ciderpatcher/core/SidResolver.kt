package com.dazaike.ciderpatcher.core

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Placeholder values for the Sid hook templates plus the methods [DexHooker] must wrap. */
class SidSymbols(val values: Map<String, String>, val hooks: List<DexHooker.Hook>)

/**
 * Re-derives the R8-obfuscated names of Cider's Sid engine (Gemini Nano gate + generate call) from
 * anchors that survive obfuscation: log/error const-strings and descriptor shapes.
 * [resolve] returns null when the build has no Sid at all.
 */
class SidResolver(private val index: DexIndex, private val base: SymbolMap) {
    private val out = LinkedHashMap<String, String>()

    fun resolve(): SidSymbols? {
        if (index.methodsWithString("checkStatus failed").isEmpty()) return null
        out.clear()
        out["F_UNIT_INSTANCE"] = base.values["F_UNIT_INSTANCE"]
            ?: throw PatchException("Could not find F_UNIT_INSTANCE (needed by Sid)")

        // 1-4: the Sid engine and its Nano status aggregator.
        val kqStatus = single("Sid.checkStatus", index.methodsWithString("checkStatus failed"))
        val kq7 = kqStatus.definingClass
        out["T_KQ7"] = kq7
        val kqCaps = single("Sid.capabilityCheck", index.methodsWithString("capability check failed").filter { it.definingClass == kq7 })
        val cont = kqCaps.params.lastOrNull() ?: single("Sid.capabilityCheck params", emptyList<String>())
        val l0c = kqCaps.params[0]
        if (kqCaps.isStatic || kqCaps.params != listOf(l0c, cont)) single("Sid.capabilityCheck shape", emptyList<String>())
        out["N_KQ7_CAPS"] = kqCaps.name
        out["T_CONT_IMPL"] = cont
        out["T_L0C"] = l0c
        val kqWarm = single("Sid.warmup", index.methodsWithString("warmup failed").filter { it.definingClass == kq7 && it.params == listOf(cont) })
        out["N_KQ7_WARMUP"] = kqWarm.name
        val tzbA = single("Nano status aggregator", kqStatus.invokeRefs(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE)
            .filter { it.params == listOf(it.definingClass, cont) && it.returnType == OBJECT }
            .distinctBy { methodDescriptor(it) })
        out["T_TZB"] = tzbA.definingClass
        out["N_TZB_STATUS"] = tzbA.name

        // 5-6: the ML Kit GenerativeModel implementation.
        val l0cInit = single("GenerativeModel impl", index.methodsWithString(".mlkit_genai_prompt_explicit_prefix_cache"))
        if (l0cInit.definingClass != l0c) {
            throw PatchException("Sid class mismatch: ${l0cInit.definingClass} vs $l0c")
        }
        val l0cD = single("Sid structured generate", index.methodsOf(l0c).filter { "Structured output feature is not available." in it.strings() })
        val l0cCalls = l0cD.invokeRefs(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE).filter { it.definingClass == l0c }
        val structuredOk = single("GenerativeModel.structuredOutputAvailable", l0cCalls.filter { it.params == listOf(cont) }.distinctBy { methodDescriptor(it) })
        val generate = single("GenerativeModel.generateContent", l0cCalls.filter {
            it.params.size == 4 && it.params[2] == "Z" && it.params[3] == cont
        }.distinctBy { methodDescriptor(it) })
        out["N_L0C_STRUCTURED_OK"] = structuredOk.name
        out["N_L0C_GENERATE"] = generate.name
        val i23 = generate.params[0]
        val kclass = generate.params[1]
        out["T_I23"] = i23
        out["T_KCLASS"] = kclass

        // 7: the request builder shows how to turn a KClass into schema text and what a part/content looks like.
        val l0cB = single("GenerativeModel request builder", index.methodsOf(l0c).filter {
            !it.isStatic && it.params.size == 2 && it.params[0] == l0cD.params[0] && it.params[1] != cont
        })
        val bInsns = l0cB.instructions()
        val mm8 = single("TextPart", bInsns.filter { it.opcode == Opcode.INSTANCE_OF }.map { typeOf(it) }.distinct())
        out["T_MM8"] = mm8
        val bStatics = l0cB.invokeRefs(Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE)
        val schemaKey = bStatics.firstOrNull { it.params == listOf(kclass) } ?: missing("schema key call")
        val schemaNode = l0cB.invokeRefs(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE)
            .firstOrNull { it.params.isEmpty() && it.definingClass == schemaKey.returnType } ?: missing("schema node call")
        val schemaText = bStatics.firstOrNull { it.params == listOf(STRING, schemaNode.returnType) && it.returnType == STRING }
            ?: missing("schema text call")
        out["M_SCHEMA_KEY"] = methodDescriptor(schemaKey)
        out["M_SCHEMA_NODE"] = methodDescriptor(schemaNode)
        out["M_SCHEMA_TEXT"] = methodDescriptor(schemaText)
        val in1 = single("Content", l0cB.invokeRefs(Opcode.INVOKE_DIRECT, Opcode.INVOKE_DIRECT_RANGE)
            .filter { it.name == "<init>" && it.params == listOf(LIST) && it.definingClass in index.classes }
            .map { it.definingClass }.distinct()
            .filter { t -> index.classes.getValue(t).instanceFields.let { f -> f.count() == 1 && f.first().type == LIST } })
        out["T_IN1"] = in1

        // 8: fields.
        out["F_I23_TEMPERATURE"] = singleField("GenerateRequest.temperature", i23) { it == "F" }
        out["F_I23_CONTENTS"] = singleField("GenerateRequest.contents", i23) { it == LIST }
        out["F_IN1_PARTS"] = singleField("Content.parts", in1) { it == LIST }
        out["F_MM8_TEXT"] = singleField("TextPart.text", mm8) { it == STRING }

        // 9: response / error / capability types, from how l0c.d and kq7.l build and read them.
        val dInsns = l0cD.instructions()
        val callIdx = dInsns.indexOfFirst { insn ->
            (insn.opcode == Opcode.INVOKE_VIRTUAL || insn.opcode == Opcode.INVOKE_VIRTUAL_RANGE) &&
                methodDescriptor((insn as ReferenceInstruction).reference as MethodReference) == methodDescriptor(generate)
        }
        val j23 = castAfter("GenerateResponse", dInsns, callIdx)
        out["T_J23"] = j23
        out["M_J23_INIT"] = initOf("GenerateResponse.<init>", j23, listOf("Ljava/util/ArrayList;"))
        val getIdx = dInsns.indexOfFirst { insn ->
            insn.opcode == Opcode.INVOKE_VIRTUAL &&
                methodDescriptor((insn as ReferenceInstruction).reference as MethodReference) == "Ljava/util/ArrayList;->get(I)Ljava/lang/Object;"
        }
        val vt0 = castAfter("Candidate", dInsns, getIdx)
        out["T_VT0"] = vt0
        out["M_VT0_INIT"] = initOf("vt0.<init>", vt0, listOf("Ljava/lang/Integer;", STRING))
        val e23Init = single("SidError ctor", l0cD.invokeRefs(Opcode.INVOKE_DIRECT, Opcode.INVOKE_DIRECT_RANGE)
            .filter { it.name == "<init>" && it.params == listOf("I", STRING, "Ljava/lang/Throwable;") && it.definingClass in index.classes }
            .distinctBy { methodDescriptor(it) })
        out["T_E23"] = e23Init.definingClass
        out["M_E23_INIT"] = methodDescriptor(e23Init)
        val to7Init = single("Capabilities ctor", kqCaps.invokeRefs(Opcode.INVOKE_DIRECT, Opcode.INVOKE_DIRECT_RANGE)
            .filter { it.name == "<init>" && it.params == listOf("Ljava/lang/Integer;", "Z", "Z", "Ljava/lang/Integer;") && it.definingClass in index.classes }
            .distinctBy { methodDescriptor(it) })
        out["T_TO7"] = to7Init.definingClass
        out["M_TO7_INIT"] = methodDescriptor(to7Init)

        // 10: Android context the hooks use to reach SharedPreferences.
        val ctxGetter = single("MlKitContext.getInstance", index.methodsWithString("MlKitContext has not been initialized")
            .filter { it.isStatic && it.params.isEmpty() && it.returnType == it.definingClass })
        val mlkit = ctxGetter.definingClass
        out["T_MLKIT_CONTEXT"] = mlkit
        out["N_MLKIT_CONTEXT_GET"] = ctxGetter.name
        out["N_MLKIT_CONTEXT_APP"] = single("MlKitContext.getApplicationContext", index.methodsOf(mlkit)
            .filter { !it.isStatic && it.params.isEmpty() && it.returnType == "Landroid/content/Context;" }).name

        val hooks = listOf(
            DexHooker.Hook(out.getValue("T_TZB"), out.getValue("N_TZB_STATUS"), listOf(out.getValue("T_TZB"), cont), OBJECT),
            DexHooker.Hook(kq7, kqCaps.name, listOf(l0c, cont), OBJECT),
            DexHooker.Hook(kq7, kqWarm.name, listOf(cont), OBJECT),
            DexHooker.Hook(l0c, structuredOk.name, listOf(cont), OBJECT),
            DexHooker.Hook(l0c, generate.name, listOf(i23, kclass, "Z", cont), OBJECT),
        )
        // The stub templates declare tzb.a static and every other hook as an instance method.
        for (h in hooks) {
            val m = single("hook target ${h.type}->${h.name}", index.methodsOf(h.type).filter { it.name == h.name && it.params == h.params && it.returnType == h.returnType })
            if (m.isStatic != (h.type == out.getValue("T_TZB"))) missing("hook target ${m.descriptor} (unexpected static flag)")
        }
        return SidSymbols(LinkedHashMap(out), hooks)
    }

    private fun <T> single(symbol: String, candidates: Collection<T>): T {
        if (candidates.size != 1) {
            throw PatchException(
                "Could not find $symbol: ${candidates.size} candidates (expected 1). " +
                    "This Cider build changed shape; the Sid patch needs updating."
            )
        }
        return candidates.first()
    }

    private fun typeOf(insn: com.android.tools.smali.dexlib2.iface.instruction.Instruction): String =
        ((insn as ReferenceInstruction).reference as com.android.tools.smali.dexlib2.iface.reference.TypeReference).type

    private fun castAfter(symbol: String, insns: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>, from: Int): String {
        if (from < 0) missing(symbol)
        val cast = insns.drop(from + 1).firstOrNull { it.opcode == Opcode.CHECK_CAST } ?: missing(symbol)
        return typeOf(cast)
    }

    private fun missing(symbol: String): Nothing =
        throw PatchException("Could not find $symbol. This Cider build changed shape; the Sid patch needs updating.")

    private fun initOf(symbol: String, type: String, params: List<String>): String =
        single(symbol, index.methodsOf(type).filter { it.name == "<init>" && it.params == params }).descriptor

    private fun singleField(symbol: String, type: String, accept: (String) -> Boolean): String {
        val cls = index.classes[type] ?: return single(symbol, emptyList<String>())
        return fieldDescriptor(single(symbol, cls.instanceFields.filter { accept(it.type) }))
    }

    private fun MethodInfo.invokeRefs(vararg ops: Opcode): List<MethodRefInfo> =
        instructions().filter { it.opcode in ops }.map { MethodRefInfo((it as ReferenceInstruction).reference as MethodReference) }

    private companion object {
        const val OBJECT = "Ljava/lang/Object;"
        const val STRING = "Ljava/lang/String;"
        const val LIST = "Ljava/util/List;"
    }
}

/** Convenience view of a [MethodReference] with string parameter types. */
private class MethodRefInfo(private val ref: MethodReference) : MethodReference by ref {
    val params: List<String> = ref.parameterTypes.map { it.toString() }
}
