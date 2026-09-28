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
     * Copies [input] to [out] in original entry order, dropping v1 signature files, swapping in [manifest],
     * and appending [newDex] as the next `classesN.dex`. STORED entries stay STORED.
     */
    fun writeUnsigned(input: File, out: File, manifest: ByteArray, newDex: ByteArray) {
        ZipFile(input).use { zip ->
            ZipOutputStream(out.outputStream().buffered()).use { zos ->
                var highestDex = 0
                for (src in zip.entries()) {
                    val name = src.name
                    if (OLD_SIGNATURE.matches(name)) continue
                    DEX_NAME.matchEntire(name)?.let { m ->
                        highestDex = maxOf(highestDex, m.groupValues[1].ifEmpty { "1" }.toInt())
                    }
                    if (name == "AndroidManifest.xml") {
                        putEntry(zos, name, manifest, src.method, src.time)
                        continue
                    }
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
                putEntry(zos, "classes${highestDex + 1}.dex", newDex, ZipEntry.STORED, System.currentTimeMillis())
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
