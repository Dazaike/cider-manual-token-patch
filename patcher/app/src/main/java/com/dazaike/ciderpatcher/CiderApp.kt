package com.dazaike.ciderpatcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.dazaike.ciderpatcher.core.CiderPatcher
import java.security.MessageDigest

/** Queries about the installed Cider and intents into it. */
object CiderApp {

    fun installedInfo(context: Context): ApplicationInfo? = try {
        context.packageManager.getApplicationInfo(CiderPatcher.TARGET_PACKAGE, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    fun hasPatchedActivity(context: Context): Boolean = try {
        context.packageManager.getActivityInfo(ComponentName(CiderPatcher.TARGET_PACKAGE, CiderPatcher.PATCH_ACTIVITY), 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** SHA-256 of the installed Cider's first signing certificate, or null if Cider isn't installed. */
    fun signerSha256(context: Context): ByteArray? = try {
        val info = context.packageManager.getPackageInfo(CiderPatcher.TARGET_PACKAGE, PackageManager.GET_SIGNING_CERTIFICATES)
        val signer = info.signingInfo?.apkContentsSigners?.firstOrNull()
        signer?.let { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()) }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /**
     * Intent for the patched activity; start it for a result (no NEW_TASK, which would cancel the result).
     * It validates the token and signs in without a dialog; see [CiderPatcher.RESULT_EXTRA] for the result contract.
     */
    fun signInIntent(token: String): Intent = Intent()
        .setClassName(CiderPatcher.TARGET_PACKAGE, CiderPatcher.PATCH_ACTIVITY)
        .putExtra(CiderPatcher.TOKEN_EXTRA, token.trim())

    /** Cider's normal launcher intent, or null if Cider isn't installed. */
    fun launchIntent(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(CiderPatcher.TARGET_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
