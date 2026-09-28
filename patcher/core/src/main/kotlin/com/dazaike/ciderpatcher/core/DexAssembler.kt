package com.dazaike.ciderpatcher.core

import com.android.tools.smali.smali.Smali
import com.android.tools.smali.smali.SmaliOptions
import java.nio.file.Files

object DexAssembler {
    /** Assembles smali sources (file name → text) into one dex. */
    fun assemble(files: Map<String, String>): ByteArray {
        val dir = Files.createTempDirectory("ciderpatch").toFile()
        try {
            val paths = files.map { (name, text) ->
                val f = dir.resolve(name)
                f.writeText(text, Charsets.UTF_8)
                f.path
            }
            val dex = dir.resolve("patch.dex")
            val options = SmaliOptions().apply {
                apiLevel = 33
                jobs = 1
                outputDexFile = dex.path
            }
            if (!Smali.assemble(options, paths)) throw PatchException("Smali assembly failed")
            return dex.readBytes()
        } finally {
            dir.deleteRecursively()
        }
    }
}
