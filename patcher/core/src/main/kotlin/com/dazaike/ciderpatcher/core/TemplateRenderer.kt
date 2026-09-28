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

    private val PLACEHOLDER = Regex("""\{\{([A-Z0-9_]+)\}\}""")

    fun loadTemplate(file: String): String {
        val stream = CiderPatcher::class.java.getResourceAsStream("/templates/$file")
            ?: throw PatchException("Missing template $file")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /** Returns file name → rendered smali source. */
    fun render(map: SymbolMap): Map<String, String> = TEMPLATE_FILES.associateWith { file ->
        val rendered = PLACEHOLDER.replace(loadTemplate(file)) { m ->
            val key = m.groupValues[1]
            map.values[key] ?: throw PatchException("Unfilled placeholder $key in $file")
        }
        if (rendered.contains("{{")) {
            val key = rendered.substringAfter("{{").substringBefore("}}")
            throw PatchException("Unfilled placeholder $key in $file")
        }
        rendered
    }
}
