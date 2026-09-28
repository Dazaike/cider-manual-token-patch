package com.dazaike.ciderpatcher.core

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.util.ReferenceUtil
import java.util.zip.ZipFile

/** Package of the classes this patch adds; excluded from the index so patched inputs resolve like stock ones. */
const val PATCH_PACKAGE_PREFIX = "Lcom/cidercollective/cider/auth/manual/"

fun methodDescriptor(ref: MethodReference): String = ReferenceUtil.getMethodDescriptor(ref)
fun fieldDescriptor(ref: FieldReference): String = ReferenceUtil.getFieldDescriptor(ref)

class MethodInfo(val method: Method) {
    val definingClass: String = method.definingClass
    val name: String = method.name
    val params: List<String> = method.parameterTypes.map { it.toString() }
    val returnType: String = method.returnType
    val isStatic: Boolean = AccessFlags.STATIC.isSet(method.accessFlags)
    val isAbstract: Boolean = AccessFlags.ABSTRACT.isSet(method.accessFlags)
    val descriptor: String get() = methodDescriptor(method)

    /** Instructions in order; re-read from the dex buffer on every call to keep the index small. */
    fun instructions(): List<Instruction> = method.implementation?.instructions?.toList() ?: emptyList()

    private fun refs(): Sequence<Any> = instructions().asSequence()
        .filterIsInstance<ReferenceInstruction>().map { it.reference }

    fun strings(): List<String> = refs().filterIsInstance<StringReference>().map { it.string }.toList()
    fun fieldRefs(): List<FieldReference> = refs().filterIsInstance<FieldReference>().toList()
    fun methodRefs(): List<MethodReference> = refs().filterIsInstance<MethodReference>().toList()
    fun typeRefs(): List<String> = refs().filterIsInstance<TypeReference>().map { it.type }.toList()

    fun invokes(target: String): Boolean = methodRefs().any { methodDescriptor(it) == target }

    fun hasOpcodeOnType(opcode: Opcode, type: String): Boolean = instructions().any {
        it.opcode == opcode && ((it as ReferenceInstruction).reference as TypeReference).type == type
    }

    fun hasFieldAccess(opcode: Opcode, field: String): Boolean = instructions().any {
        it.opcode == opcode && fieldDescriptor((it as ReferenceInstruction).reference as FieldReference) == field
    }

    override fun toString() = descriptor
}

class DexIndex(dexFiles: List<DexBackedDexFile>) {
    val classes: Map<String, ClassDef>
    val methods: List<MethodInfo>
    val methodsByClass: Map<String, List<MethodInfo>>
    private val stringIndex: Map<String, List<MethodInfo>>

    init {
        val classMap = LinkedHashMap<String, ClassDef>()
        for (dex in dexFiles) for (cls in dex.classes) {
            if (cls.type.startsWith(PATCH_PACKAGE_PREFIX)) continue
            classMap.putIfAbsent(cls.type, cls)
        }
        classes = classMap
        val all = ArrayList<MethodInfo>()
        val byClass = HashMap<String, List<MethodInfo>>(classMap.size * 2)
        val strings = HashMap<String, MutableList<MethodInfo>>()
        for (cls in classMap.values) {
            val list = cls.methods.map { MethodInfo(it) }
            byClass[cls.type] = list
            all += list
            for (m in list) for (s in m.strings().distinct()) strings.getOrPut(s) { ArrayList(2) } += m
        }
        methods = all
        methodsByClass = byClass
        stringIndex = strings
    }

    fun methodsOf(type: String): List<MethodInfo> = methodsByClass[type].orEmpty()
    fun staticFieldsOf(type: String): List<Field> = classes[type]?.staticFields?.toList().orEmpty()
    fun isAbstractClass(type: String): Boolean = classes[type]?.let { AccessFlags.ABSTRACT.isSet(it.accessFlags) } == true

    /** Methods containing a const-string exactly equal to [s]. */
    fun methodsWithString(s: String): List<MethodInfo> = stringIndex[s].orEmpty()

    /** Methods containing a const-string that contains [sub]. */
    fun methodsWithStringContaining(sub: String): List<MethodInfo> =
        stringIndex.entries.filter { it.key.contains(sub) }.flatMap { it.value }.distinct()

    companion object {
        private val DEX_ENTRY = Regex("""classes\d*\.dex""")

        /** Raw bytes of every top-level `classes*.dex` entry, in zip order. */
        fun readDexEntries(zip: ZipFile): List<ByteArray> = zip.entries().asSequence()
            .filter { DEX_ENTRY.matches(it.name) }
            .map { e -> zip.getInputStream(e).use { it.readBytes() } }
            .toList()

        fun parse(dexBytes: List<ByteArray>): List<DexBackedDexFile> =
            dexBytes.map { DexBackedDexFile(Opcodes.getDefault(), it) }
    }
}
