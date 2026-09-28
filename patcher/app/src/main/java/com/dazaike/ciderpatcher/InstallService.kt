package com.dazaike.ciderpatcher

import android.os.ParcelFileDescriptor
import kotlin.system.exitProcess

/** Shizuku user service; runs in a separate process as the shell uid, so `pm` can install without prompts. */
class InstallService : IInstallService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun install(apk: ParcelFileDescriptor, sizeBytes: Long): String {
        val process = ProcessBuilder("pm", "install", "-r", "-S", sizeBytes.toString())
            .redirectErrorStream(true)
            .start()
        ParcelFileDescriptor.AutoCloseInputStream(apk).use { input ->
            process.outputStream.use { input.copyTo(it) }
        }
        return finish(process)
    }

    override fun uninstall(packageName: String): String {
        val process = ProcessBuilder("pm", "uninstall", packageName).redirectErrorStream(true).start()
        process.outputStream.close()
        return finish(process)
    }

    private fun finish(process: Process): String {
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        return output.trim()
    }
}
