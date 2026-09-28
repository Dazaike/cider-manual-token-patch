package com.dazaike.ciderpatcher.core

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import java.io.File
import java.util.zip.ZipFile

object CiderPatcher {
    const val TARGET_PACKAGE = "com.cidercollective.cider"
    const val PATCH_ACTIVITY = "com.cidercollective.cider.auth.manual.ManualAppleTokenActivity"
    /** Must match the const-string in the ManualAppleTokenActivity template's token-extra block. */
    const val TOKEN_EXTRA = "music_user_token"

    /**
     * Result contract of the patched activity when started for a result: `RESULT_OK` after a successful
     * sign-in; otherwise result code 1 (`RESULT_FIRST_USER`) with [RESULT_EXTRA] set to [RESULT_INVALID]
     * (Apple rejected the token) or [RESULT_UNVERIFIED] (Cider couldn't check it, e.g. no Cider account
     * sign-in or offline). Must match the SignInCoroutine template's sign-in-result block.
     */
    const val RESULT_EXTRA = "sign_in_result"
    const val RESULT_INVALID = "invalid"
    const val RESULT_UNVERIFIED = "unverified"

    private const val PATCH_ACTIVITY_TYPE = "Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity;"

    enum class Step(val label: String) {
        READ("Reading APK…"),
        RESOLVE("Resolving symbols…"),
        ASSEMBLE("Assembling patch classes…"),
        CHECK("Checking references…"),
        WRITE("Writing APK…"),
        SIGN("Signing…"),
    }

    /**
     * Patches the full Cider APK [input] and writes the signed result to [output].
     * [onStep] fires as each [Step] starts; [log] receives each step label, every resolved symbol, and a final `Done`.
     */
    fun patch(
        input: File,
        output: File,
        key: SigningKey,
        log: (String) -> Unit = {},
        onStep: (Step) -> Unit = {},
    ) {
        val unsigned = File(output.path + ".unsigned")
        fun step(s: Step) {
            onStep(s)
            log(s.label)
        }
        try {
            step(Step.READ)
            val (manifest, dexFiles) = ZipFile(input).use { zip ->
                val entry = zip.getEntry("AndroidManifest.xml") ?: throw PatchException("Not an APK")
                val manifest = ManifestPatcher.patch(zip.getInputStream(entry).use { it.readBytes() })
                manifest to DexIndex.parse(DexIndex.readDexEntries(zip))
            }
            if (dexFiles.any { dex -> dex.classes.any { it.type == PATCH_ACTIVITY_TYPE } }) {
                throw PatchException("This APK is already patched")
            }

            step(Step.RESOLVE)
            val index = DexIndex(dexFiles)
            val symbols = SymbolResolver(index).resolve()
            for ((k, v) in symbols.values) log("$k → $v")

            step(Step.ASSEMBLE)
            val newDex = DexAssembler.assemble(TemplateRenderer.render(symbols))

            step(Step.CHECK)
            LinkChecker.check(DexBackedDexFile(Opcodes.getDefault(), newDex), index)

            step(Step.WRITE)
            ApkWriter.writeUnsigned(input, unsigned, manifest, newDex)

            step(Step.SIGN)
            ApkSigning.sign(unsigned, output, key)
            log("Done")
        } finally {
            unsigned.delete()
        }
    }
}
