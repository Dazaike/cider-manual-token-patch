package com.dazaike.ciderpatcher.core

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/**
 * Re-derives every R8-obfuscated name the patch templates need from a Cider build, using
 * anchors (const-strings, descriptors, class shapes) that survive obfuscation.
 */
class SymbolResolver(private val index: DexIndex) {
    private val out = LinkedHashMap<String, String>()

    fun resolve(): SymbolMap {
        out.clear()
        val order = resolveCoroutineBase()
        resolveCoroutineSuspended()
        resolveUnit()
        resolveThrowOnFailure()
        val (store, keyType) = resolveSecureTokenStore()
        resolveDataStoreEdit(store, keyType)
        resolveMutablePrefsSet(keyType)
        resolveRunBlocking()
        resolveCiderRuntime()
        val api = resolveAppleMusicApi()
        resolveUserTokenProbe(api)
        resolveMarkOnboardingCompleted()
        checkShapes()
        return SymbolMap.withDerived(out, order)
    }

    private fun <T> single(symbol: String, candidates: Collection<T>): T {
        if (candidates.size != 1) {
            throw PatchException(
                "Could not find $symbol: ${candidates.size} candidates (expected 1). " +
                    "This Cider build changed shape; the patch needs updating."
            )
        }
        return candidates.first()
    }

    private fun sig(m: MethodInfo, params: List<String>, ret: String) = m.params == params && m.returnType == ret

    // Rule 1: BaseContinuationImpl.create(Any?, Continuation) stub.
    private fun resolveCoroutineBase(): CreateOrder {
        val stub = single("BaseContinuationImpl.create", index.methodsWithString("create(Any?;Continuation) has not been overridden"))
        val cont = stub.returnType
        val order = when (stub.params) {
            listOf(cont, OBJECT) -> CreateOrder.CONT_FIRST
            listOf(OBJECT, cont) -> CreateOrder.VALUE_FIRST
            else -> throw PatchException(
                "BaseContinuationImpl.create changed from (Continuation, Object) to ${stub.params}; the patch needs updating."
            )
        }
        val base = stub.definingClass
        val invokeSuspend = single("BaseContinuationImpl.invokeSuspend",
            index.methodsOf(base).filter { it.isAbstract && sig(it, listOf(OBJECT), OBJECT) })
        out["T_CONTINUATION"] = cont
        out["N_CREATE"] = stub.name
        out["N_INVOKE_SUSPEND"] = invokeSuspend.name

        // Rule 2: ContinuationImpl.
        val contImpl = single("ContinuationImpl", index.classes.values.filter { cls ->
            cls.superclass == base && AccessFlags.ABSTRACT.isSet(cls.accessFlags) &&
                index.methodsOf(cls.type).any { it.name == "<init>" && it.params.size == 2 && it.params[0] == cont }
        }).type

        // Rule 3: SuspendLambda.
        val lambdaInit = single("SuspendLambda", index.classes.values.filter { cls ->
            cls.superclass == contImpl && AccessFlags.ABSTRACT.isSet(cls.accessFlags)
        }.flatMap { cls -> index.methodsOf(cls.type).filter { it.name == "<init>" && sig(it, listOf("I", cont), "V") } })
        out["T_SUSPEND_LAMBDA"] = lambdaInit.definingClass
        out["M_SUSPEND_LAMBDA_INIT"] = lambdaInit.descriptor
        return order
    }

    // Rule 4.
    private fun resolveCoroutineSuspended() {
        val clinit = single("CoroutineSingletons", index.methodsWithString("COROUTINE_SUSPENDED").filter { it.name == "<clinit>" })
        out["F_COROUTINE_SUSPENDED"] = sputAfterString("COROUTINE_SUSPENDED", clinit, "COROUTINE_SUSPENDED") {
            it.type == clinit.definingClass
        }
    }

    // Rule 5.
    private fun resolveUnit() {
        val toString = single("Unit", index.methodsWithString("kotlin.Unit").filter { isToString(it) })
        out["F_UNIT_INSTANCE"] = ownInstanceField("Unit.INSTANCE", toString.definingClass)
    }

    // Rule 6.
    private fun resolveThrowOnFailure() {
        val failure = single("Result.Failure", index.methodsWithString("Failure(").filter { isToString(it) }).definingClass
        val m = single("ResultKt.throwOnFailure", index.methods.filter {
            sig(it, listOf(OBJECT), "V") && run {
                val insns = it.instructions()
                insns.size in 1..10 && insns.any { i -> i.opcode == Opcode.THROW } &&
                    it.hasOpcodeOnType(Opcode.INSTANCE_OF, failure)
            }
        })
        out["M_THROW_ON_FAILURE"] = m.descriptor
    }

    // Rule 7. Returns (store class, preference key type).
    private fun resolveSecureTokenStore(): Pair<String, String> {
        val clinit = single("SecureTokenStore", index.methodsWithString("music_user_token").filter {
            it.name == "<clinit>" && "tink_keyset" in it.strings()
        })
        val store = clinit.definingClass
        var keyType = ""
        out["F_KEY_MUSIC_USER_TOKEN"] = sputAfterString("SecureTokenStore.KEY_MUSIC_USER_TOKEN", clinit, "music_user_token") {
            (it.definingClass == store).also { ok -> if (ok) keyType = it.type }
        }
        out["F_KEY_STOREFRONT"] = sputAfterString("SecureTokenStore.KEY_STOREFRONT", clinit, "storefront") {
            it.definingClass == store
        }
        val encrypt = single("SecureTokenStore.encrypt", index.methodsOf(store).filter {
            sig(it, listOf(STRING), STRING) && it.invokes("Ljava/util/Base64;->getEncoder()Ljava/util/Base64\$Encoder;")
        })
        out["M_ENCRYPT"] = encrypt.descriptor
        return store to keyType
    }

    // Rule 8.
    private fun resolveDataStoreEdit(store: String, keyType: String) {
        val cont = out.getValue("T_CONTINUATION")
        val storeFieldTypes = index.staticFieldsOf(store).map { it.type }.filter { it != keyType }.toSet()
        val matches = index.methods.filter { m ->
            m.params.size == 3 && m.params[0] in storeFieldTypes && m.params[2] == cont && m.returnType == OBJECT &&
                m.instructions().any { i ->
                    (i.opcode == Opcode.INVOKE_INTERFACE || i.opcode == Opcode.INVOKE_INTERFACE_RANGE) &&
                        ((i as ReferenceInstruction).reference as MethodReference).definingClass == m.params[0]
                }
        }
        val edit = single("DataStore.edit", matches)
        val dataStore = edit.params[0]
        val function2 = edit.params[1]
        out["M_PREFS_EDIT"] = edit.descriptor
        out["T_FUNCTION2"] = function2
        out["F_DATASTORE"] = fieldDescriptor(single("SecureTokenStore.dataStore",
            index.staticFieldsOf(store).filter { it.type == dataStore }))
        out["N_FUNCTION2_INVOKE"] = single("Function2.invoke", index.methodsOf(function2).filter {
            it.isAbstract && sig(it, listOf(OBJECT, OBJECT), OBJECT)
        }).name
    }

    // Rule 9.
    private fun resolveMutablePrefsSet(keyType: String) {
        val cls = single("MutablePreferences",
            index.methodsWithString("Do mutate preferences once returned to DataStore.").map { it.definingClass }.distinct())
        val candidates = index.methodsOf(cls).filter { sig(it, listOf(keyType, OBJECT), "V") }
        val set = when (candidates.size) {
            1 -> candidates[0]
            2 -> single("MutablePreferences.set", candidates.filter { c ->
                val other = candidates.first { it !== c }.descriptor
                c.invokes(other)
            })
            else -> single("MutablePreferences.set", candidates)
        }
        out["T_MUTABLE_PREFS"] = cls
        out["M_PREFS_SET"] = set.descriptor
    }

    // Rule 10.
    private fun resolveRunBlocking() {
        val function2 = out.getValue("T_FUNCTION2")
        val real = single("runBlocking", index.methods.filter { m ->
            m.isStatic && m.params.size == 2 && m.params[1] == function2 && m.returnType == OBJECT &&
                m.invokes("Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;") &&
                m.hasOpcodeOnType(Opcode.NEW_INSTANCE, "Ljava/lang/InterruptedException;")
        })
        // R8 may emit identical $default bridges in several classes (Cider 1.0.97: two); any works,
        // so prefer the one beside the real method.
        val bridges = index.methods.filter { m ->
            sig(m, listOf(function2), OBJECT) && m.invokes(real.descriptor)
        }
        val beside = bridges.filter { it.definingClass == real.definingClass }
        out["M_RUN_BLOCKING_DEFAULT"] = single("runBlocking\$default", if (bridges.size > 1) beside else bridges).descriptor
    }

    // Rule 11.
    private fun resolveCiderRuntime() {
        val secure = index.methodsWithStringContaining("secureToken").toSet()
        val ensure = single("CiderRuntime.ensureInitialized", index.methodsWithStringContaining("initialised in ").filter {
            it in secure && sig(it, listOf("Landroid/content/ContextWrapper;"), "V")
        })
        val runtime = ensure.definingClass
        val target = ensure.descriptor
        val fields = LinkedHashSet<String>()
        var recorded = 0
        for (m in index.methods) {
            val insns = m.instructions()
            for ((i, insn) in insns.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name != ensure.name || ref.definingClass != runtime || methodDescriptor(ref) != target) continue
                val receiver = when (insn) {
                    is FiveRegisterInstruction -> insn.registerC
                    is RegisterRangeInstruction -> insn.startRegister
                    else -> continue
                }
                val setter = (i - 1 downTo 0).map { insns[it] }.firstOrNull {
                    it.opcode.setsRegister() && it is OneRegisterInstruction && it.registerA == receiver
                } ?: continue
                if (setter.opcode != Opcode.SGET_OBJECT) continue
                val field = (setter as ReferenceInstruction).reference as FieldReference
                if (field.type != runtime) continue
                fields += fieldDescriptor(field)
                recorded++
            }
        }
        if (recorded == 0) throw PatchException(
            "Could not find CiderRuntime.INSTANCE: 0 candidates (expected 1). This Cider build changed shape; the patch needs updating."
        )
        out["M_ENSURE_INITIALIZED"] = target
        out["F_CIDER_RUNTIME_INSTANCE"] = single("CiderRuntime.INSTANCE", fields)
    }

    // Rule 12. Returns the AppleMusicApi class.
    private fun resolveAppleMusicApi(): String {
        val m = single("AppleMusicApi.getUserStorefront", index.methodsWithStringContaining("getUserStorefront failed").filter {
            it.params.size == 1 && it.returnType == OBJECT
        })
        out["M_GET_USER_STOREFRONT"] = m.descriptor
        out["F_APPLE_MUSIC_API_INSTANCE"] = ownInstanceField("AppleMusicApi.INSTANCE", m.definingClass)
        return m.definingClass
    }

    // Rule 13.
    private fun resolveUserTokenProbe(api: String) {
        val clinit = single("UserTokenProbe", index.methodsWithString("INDETERMINATE").filter {
            it.name == "<clinit>" && index.classes[it.definingClass]?.let { c -> AccessFlags.ENUM.isSet(c.accessFlags) } == true &&
                it.strings().containsAll(listOf("VALID", "INVALID"))
        })
        val probe = clinit.definingClass
        val valid = sputAfterString("UserTokenProbe.VALID", clinit, "VALID") { it.type == probe && it.definingClass == probe }
        out["T_USER_TOKEN_PROBE"] = probe
        out["F_PROBE_VALID"] = valid
        out["F_PROBE_INVALID"] = sputAfterString("UserTokenProbe.INVALID", clinit, "INVALID") {
            it.type == probe && it.definingClass == probe
        }
        out["M_PROBE_MUSIC_USER_TOKEN"] = single("AppleMusicApi.probeMusicUserToken", index.methodsOf(api).filter {
            it.params.size == 2 && it.params[0] == STRING && it.hasFieldAccess(Opcode.SGET_OBJECT, valid)
        }).descriptor
    }

    // Rule 14.
    private fun resolveMarkOnboardingCompleted() {
        var candidates = index.methodsWithString("onboarding_completed").filter { sig(it, emptyList(), "V") }
        if (candidates.size > 1) {
            candidates = candidates.filter { m ->
                m.fieldRefs().any { fieldDescriptor(it) == "Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;" }
            }
        }
        out["M_MARK_ONBOARDING_COMPLETED"] = single("AppSettings.markOnboardingCompleted", candidates).descriptor
    }

    private fun checkShapes() {
        val byDescriptor = HashMap<String, MethodInfo>()
        val wanted = STATIC_METHODS.keys + INSTANCE_METHODS.keys
        for (key in wanted) {
            val desc = out.getValue(key)
            val cls = desc.substringBefore("->")
            byDescriptor[key] = index.methodsOf(cls).first { it.descriptor == desc }
        }
        for ((key, symbol) in STATIC_METHODS) if (!byDescriptor.getValue(key).isStatic) {
            throw PatchException("$symbol changed from static to instance; the patch needs updating.")
        }
        for ((key, symbol) in INSTANCE_METHODS) if (byDescriptor.getValue(key).isStatic) {
            throw PatchException("$symbol changed from instance to static; the patch needs updating.")
        }
    }

    private fun isToString(m: MethodInfo) = m.name == "toString" && sig(m, emptyList(), STRING)

    private fun ownInstanceField(symbol: String, type: String): String =
        fieldDescriptor(single(symbol, index.staticFieldsOf(type).filter { it.type == type }))

    /** First `sput-object` after `const-string [s]` in [m] whose field satisfies [accept]. */
    private fun sputAfterString(symbol: String, m: MethodInfo, s: String, accept: (FieldReference) -> Boolean): String {
        val insns: List<Instruction> = m.instructions()
        val start = insns.indexOfFirst {
            (it.opcode == Opcode.CONST_STRING || it.opcode == Opcode.CONST_STRING_JUMBO) &&
                ((it as ReferenceInstruction).reference as StringReference).string == s
        }
        if (start >= 0) {
            for (insn in insns.subList(start + 1, insns.size)) {
                if (insn.opcode != Opcode.SPUT_OBJECT) continue
                val field = (insn as ReferenceInstruction).reference as FieldReference
                if (accept(field)) return fieldDescriptor(field)
            }
        }
        return single(symbol, emptyList<String>())
    }

    companion object {
        private const val OBJECT = "Ljava/lang/Object;"
        private const val STRING = "Ljava/lang/String;"

        private val STATIC_METHODS = mapOf(
            "M_ENCRYPT" to "SecureTokenStore.encrypt",
            "M_THROW_ON_FAILURE" to "ResultKt.throwOnFailure",
            "M_PREFS_EDIT" to "DataStore.edit",
            "M_RUN_BLOCKING_DEFAULT" to "runBlocking\$default",
            "M_MARK_ONBOARDING_COMPLETED" to "AppSettings.markOnboardingCompleted",
        )
        private val INSTANCE_METHODS = mapOf(
            "M_PROBE_MUSIC_USER_TOKEN" to "AppleMusicApi.probeMusicUserToken",
            "M_GET_USER_STOREFRONT" to "AppleMusicApi.getUserStorefront",
            "M_ENSURE_INITIALIZED" to "CiderRuntime.ensureInitialized",
            "M_PREFS_SET" to "MutablePreferences.set",
        )
    }
}
