package com.dazaike.ciderpatcher.core

import com.android.apksig.ApkSigner
import java.io.File

object ApkSigning {
    /** v2+v3 signs [unsigned] into [out]; apksig also aligns STORED entries (4 bytes) and `.so` files (16 KiB). */
    fun sign(unsigned: File, out: File, key: SigningKey) {
        val signer = ApkSigner.SignerConfig.Builder("cider-patcher", key.privateKey, listOf(key.certificate)).build()
        ApkSigner.Builder(listOf(signer))
            .setInputApk(unsigned)
            .setOutputApk(out)
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setLibraryPageAlignmentBytes(16384)
            .build()
            .sign()
    }
}
