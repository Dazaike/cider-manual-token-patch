package com.dazaike.ciderpatcher

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Optional on-device storage for the user's Cider license key and Music-User-Token. Values are AES-GCM
 * encrypted with a non-exportable Android Keystore key before they touch SharedPreferences.
 */
object SecretStore {
    const val LICENSE_KEY = "license_key"
    const val MUSIC_USER_TOKEN = "music_user_token"

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "cider-patcher-secrets"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFS = "secrets"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply { init(spec) }.generateKey()
    }

    /** Stores [value] encrypted under [name]; a blank value removes it. */
    fun put(context: Context, name: String, value: String) {
        if (value.isBlank()) {
            prefs(context).edit().remove(name).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(value.trim().toByteArray(Charsets.UTF_8))
        prefs(context).edit().putString(name, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
    }

    /** The decrypted value, or null if none is stored or it can no longer be decrypted. */
    fun get(context: Context, name: String): String? {
        val encoded = prefs(context).getString(name, null) ?: return null
        return runCatching {
            val sealed = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, sealed, 0, IV_BYTES))
            String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES), Charsets.UTF_8)
        }.getOrElse {
            // Keystore key lost (e.g. restored backup); the stored value is unreadable.
            prefs(context).edit().remove(name).apply()
            null
        }
    }

    private const val IV_BYTES = 12
}
