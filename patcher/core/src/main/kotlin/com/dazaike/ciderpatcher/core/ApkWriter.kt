package com.dazaike.ciderpatcher.core

import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkWriter {
    private val OLD_SIGNATURE = Regex("""^META-INF/([^/]+\.(SF|RSA|DSA|EC)|MANIFEST\.MF)$""")
    private val DEX_NAME = Regex("""^classes(\d*)\.dex$""")

    /**
     * Copies [input] to [out] in original entry order, dropping v1 signature files and swapping in [manifest].
     * When [frontDex] is non-null it becomes `classes.dex` (first definition wins at class load) and every
     * original `classesN.dex` shifts to `classes{N+1}.dex`. Each of [extraDexes] is then appended as the next
     * `classesN.dex`. STORED entries stay STORED.
     */
    fun writeUnsigned(input: File, out: File, manifest: ByteArray, frontDex: ByteArray?, extraDexes: List<ByteArray>) {
        ZipFile(input).use { zip ->
            var highestDex = 0
            for (src in zip.entries()) {
                DEX_NAME.matchEntire(src.name)?.let { m ->
                    highestDex = maxOf(highestDex, m.groupValues[1].ifEmpty { "1" }.toInt())
                }
            }
            if (frontDex != null) highestDex++
            ZipOutputStream(out.outputStream().buffered()).use { zos ->
                if (frontDex != null) putEntry(zos, "classes.dex", frontDex, ZipEntry.STORED, System.currentTimeMillis())
                for (src in zip.entries()) {
                    val srcName = src.name
                    if (OLD_SIGNATURE.matches(srcName)) continue
                    if (srcName == "AndroidManifest.xml") {
                        putEntry(zos, srcName, manifest, src.method, src.time)
                        continue
                    }
                    val name = if (frontDex != null) {
                        DEX_NAME.matchEntire(srcName)?.let { m ->
                            "classes${m.groupValues[1].ifEmpty { "1" }.toInt() + 1}.dex"
                        } ?: srcName
                    } else srcName
                    val entry = ZipEntry(name).apply {
                        method = src.method
                        time = src.time
                        if (src.method == ZipEntry.STORED) {
                            size = src.size
                            compressedSize = src.compressedSize
                            crc = src.crc
                        }
                    }
                    zos.putNextEntry(entry)
                    zip.getInputStream(src).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                extraDexes.forEachIndexed { i, dex ->
                    putEntry(zos, "classes${highestDex + 1 + i}.dex", dex, ZipEntry.STORED, System.currentTimeMillis())
                }
            }
        }
    }

    private fun putEntry(zos: ZipOutputStream, name: String, data: ByteArray, method: Int, time: Long) {
        val entry = ZipEntry(name).apply {
            this.method = method
            this.time = time
            if (method == ZipEntry.STORED) {
                size = data.size.toLong()
                compressedSize = data.size.toLong()
                crc = CRC32().apply { update(data) }.value
            }
        }
        zos.putNextEntry(entry)
        zos.write(data)
        zos.closeEntry()
    }
}
