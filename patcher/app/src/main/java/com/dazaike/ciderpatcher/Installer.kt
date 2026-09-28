package com.dazaike.ciderpatcher

import java.io.File

/** How the patched (or original) Cider gets installed. */
enum class InstallMethod { SHIZUKU, SYSTEM }

interface Installer {
    /** Installs or updates [apk]. Returns the installer's output; success when it contains `Success`. */
    suspend fun install(apk: File): String

    /** Uninstalls [packageName], then installs [apk]. Returns the failing step's output or the install output. */
    suspend fun uninstallThenInstall(packageName: String, apk: File): String
}
