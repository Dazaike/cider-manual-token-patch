package com.dazaike.ciderpatcher.core

object TemplateRenderer {
    val TEMPLATE_FILES = listOf(
        "ManualAppleTokenActivity.smali",
        "ManualAppleTokenActivity\$NegativeClickListener.smali",
        "ManualAppleTokenActivity\$OnShowListener.smali",
        "ManualAppleTokenActivity\$PositiveClickListener.smali",
        "ManualAppleTokenActivity\$SignInThread.smali",
        "SaveStorefrontEdit.smali",
        "SaveTokenEdit.smali",
        "SignInCoroutine.smali",
        "SignInCoroutine\$FinishRunnable.smali",
        "SignInCoroutine\$InvalidTokenRunnable.smali",
    )
    val SID_TEMPLATE_FILES = listOf(
        "HookTzb.smali",
        "HookKq7.smali",
        "HookL0c.smali",
        "SidGlue.smali",
    )

    private val PLACEHOLDER = Regex("""\{\{([A-Z0-9_]+)\}\}""")

    fun loadTemplate(file: String): String = load("/templates/$file", file)

    private fun load(path: String, file: String): String {
        val stream = CiderPatcher::class.java.getResourceAsStream(path)
            ?: throw PatchException("Missing template $file")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /** Returns file name → rendered smali source. */
    fun render(map: SymbolMap): Map<String, String> =
        TEMPLATE_FILES.associateWith { file -> substitute(loadTemplate(file), file, map.values) }

    /** Same as [render] for the Sid hook stubs under `templates/sid/`. */
    fun renderSid(values: Map<String, String>): Map<String, String> =
        SID_TEMPLATE_FILES.associateWith { file -> substitute(load("/templates/sid/$file", file), file, values) }

    private fun substitute(template: String, file: String, values: Map<String, String>): String {
        val rendered = PLACEHOLDER.replace(template) { m ->
            val key = m.groupValues[1]
            values[key] ?: throw PatchException("Unfilled placeholder $key in $file")
        }
        if (rendered.contains("{{")) {
            val key = rendered.substringAfter("{{").substringBefore("}}")
            throw PatchException("Unfilled placeholder $key in $file")
        }
        return rendered
    }
}
