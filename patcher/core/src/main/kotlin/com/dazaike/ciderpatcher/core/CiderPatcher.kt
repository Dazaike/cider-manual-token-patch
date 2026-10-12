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

    /** Must match the const-strings in the ManualAppleTokenActivity template's sid-config-extra block. */
    const val SID_API_KEY_EXTRA = "sid_gemini_api_key"
    const val SID_MODEL_EXTRA = "sid_gemini_model"
    const val SID_LOG_APPLIED = "Sid cloud: applied"
    const val SID_LOG_SKIPPED_PREFIX = "Sid cloud: skipped"

    /** English-only copy tweak: Sid's intro claimed everything stays on the phone. Skipped when the text isn't found. */
    private val SID_FOOTNOTE = Pair(
        "What you type never leaves this phone: I think with Gemini Nano, right here. I only ask Apple Music about songs, and nothing in your library changes until you tap Done or Create playlist.",
        "What you type stays between you and my model: Gemini Nano on this phone, or Gemini in the cloud when you've added an API key. I only ask Apple Music about songs, and nothing in your library changes until you tap Done or Create playlist.",
    )

    private const val PATCH_ACTIVITY_TYPE = "Lcom/cidercollective/cider/auth/manual/ManualAppleTokenActivity;"

    enum class Step(val label: String) {
        READ("Reading APK…"),
        RESOLVE("Resolving symbols…"),
        ASSEMBLE("Assembling patch classes…"),
        HOOK("Hooking Sid…"),
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
            // A Sid failure must never abort the token patch.
            val sid = try {
                SidResolver(index, symbols).resolve()
                    ?: null.also { log("$SID_LOG_SKIPPED_PREFIX (this Cider build has no Sid)") }
            } catch (e: PatchException) {
                log("$SID_LOG_SKIPPED_PREFIX (${e.message})")
                null
            }
            if (sid != null) for ((k, v) in sid.values) log("$k → $v")

            step(Step.ASSEMBLE)
            val newDex = DexAssembler.assemble(TemplateRenderer.render(symbols))
            val cloudDex = CiderPatcher::class.java.getResourceAsStream("/cloudsid/classes.dex")?.use { it.readBytes() }
                ?: throw PatchException("Missing cloud client dex")

            step(Step.HOOK)
            val frontDex = if (sid == null) null else try {
                val stubDex = DexAssembler.assemble(TemplateRenderer.renderSid(sid.values))
                val stubs = DexBackedDexFile(Opcodes.getDefault(), stubDex)
                LinkChecker.check(stubs, index)
                DexHooker.build(index, sid.hooks, stubs, SID_FOOTNOTE).also { log(SID_LOG_APPLIED) }
            } catch (e: PatchException) {
                log("$SID_LOG_SKIPPED_PREFIX (${e.message})")
                null
            }

            step(Step.CHECK)
            LinkChecker.check(DexBackedDexFile(Opcodes.getDefault(), newDex), index)

            step(Step.WRITE)
            ApkWriter.writeUnsigned(input, unsigned, manifest, frontDex, listOf(newDex, cloudDex))

            step(Step.SIGN)
            ApkSigning.sign(unsigned, output, key)
            log("Done")
        } finally {
            unsigned.delete()
        }
    }
}
