package com.dazaike.ciderpatcher

import android.content.Context
import com.dazaike.ciderpatcher.core.SigningKey
import java.io.File

/** One signing key per install of the patcher, so every patched Cider update installs over the previous one. */
object SigningKeyStore {
    @Volatile private var cached: SigningKey? = null

    @Synchronized
    fun get(context: Context): SigningKey {
        cached?.let { return it }
        val file = File(context.filesDir, "signing.p12")
        val key = if (file.exists()) {
            SigningKey.loadFrom(file)
        } else {
            SigningKey.generate().also { it.saveTo(file) }
        }
        cached = key
        return key
    }
}
