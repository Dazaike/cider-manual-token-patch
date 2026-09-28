package com.dazaike.ciderpatcher

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Installs through Android's own package installer, like tapping an APK file: the system shows its
 * install (and uninstall) confirmation dialogs. Results come back through a one-shot status broadcast.
 */
class SystemInstaller(private val context: Context) : Installer {
    private val installer get() = context.packageManager.packageInstaller

    override suspend fun install(apk: File): String {
        val sessionId = withContext(Dispatchers.IO) {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setSize(apk.length())
            }
            val id = installer.createSession(params)
            installer.openSession(id).use { session ->
                session.openWrite("base.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
            }
            id
        }
        return awaitStatus { sender -> installer.openSession(sessionId).use { it.commit(sender) } }
    }

    override suspend fun uninstallThenInstall(packageName: String, apk: File): String {
        val removed = awaitStatus { sender -> installer.uninstall(packageName, sender) }
        if (!removed.contains("Success")) return removed
        return install(apk)
    }

    /**
     * Runs [start] with an IntentSender for a private status broadcast and suspends until the final status.
     * `STATUS_PENDING_USER_ACTION` launches the system confirmation dialog and keeps waiting.
     */
    private suspend fun awaitStatus(start: (android.content.IntentSender) -> Unit): String =
        suspendCancellableCoroutine { cont ->
            val action = "${context.packageName}.INSTALL_STATUS.${requestIds.incrementAndGet()}"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                    if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)?.let { confirm ->
                            context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        return
                    }
                    runCatching { context.unregisterReceiver(this) }
                    val result = when (status) {
                        PackageInstaller.STATUS_SUCCESS -> "Success"
                        PackageInstaller.STATUS_FAILURE_ABORTED -> "Cancelled."
                        else -> intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            ?: "Failed (status $status)."
                    }
                    if (cont.isActive) cont.resume(result)
                }
            }
            context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
            cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                Intent(action).setPackage(context.packageName),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            try {
                start(pending.intentSender)
            } catch (e: Exception) {
                runCatching { context.unregisterReceiver(receiver) }
                if (cont.isActive) cont.resume(e.message ?: e.toString())
            }
        }

    private companion object {
        val requestIds = AtomicInteger()
    }
}
