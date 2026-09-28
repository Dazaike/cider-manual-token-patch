package com.dazaike.ciderpatcher.core

import org.junit.Assume
import java.io.File

object TestFixtures {
    /** Cider v1.0.93 R8 names (tables A/B of the plan plus own method names). */
    val V93: Map<String, String> = mapOf(
        "M_SUSPEND_LAMBDA_INIT" to "Lc/a27;-><init>(ILc/qb1;)V",
        "F_KEY_MUSIC_USER_TOKEN" to "Lc/aa6;->a:Lc/fi5;",
        "F_KEY_STOREFRONT" to "Lc/aa6;->b:Lc/fi5;",
        "M_ENCRYPT" to "Lc/aa6;->c(Ljava/lang/String;)Ljava/lang/String;",
        "F_DATASTORE" to "Lc/aa6;->i:Lc/yf1;",
        "F_COROUTINE_SUSPENDED" to "Lc/bd1;->l:Lc/bd1;",
        "F_APPLE_MUSIC_API_INSTANCE" to "Lc/cs;->a:Lc/cs;",
        "M_GET_USER_STOREFRONT" to "Lc/cs;->n0(Lc/rb1;)Ljava/lang/Object;",
        "M_PROBE_MUSIC_USER_TOKEN" to "Lc/cs;->t0(Ljava/lang/String;Lc/rb1;)Ljava/lang/Enum;",
        "F_PROBE_VALID" to "Lc/dp;->l:Lc/dp;",
        "F_PROBE_INVALID" to "Lc/dp;->m:Lc/dp;",
        "M_THROW_ON_FAILURE" to "Lc/g38;->i(Ljava/lang/Object;)V",
        "M_PREFS_SET" to "Lc/kk4;->e(Lc/fi5;Ljava/lang/Object;)V",
        "M_MARK_ONBOARDING_COMPLETED" to "Lc/mn;->s()V",
        "M_PREFS_EDIT" to "Lc/oa0;->f(Lc/yf1;Lc/zh2;Lc/qb1;)Ljava/lang/Object;",
        "M_RUN_BLOCKING_DEFAULT" to "Lc/pe8;->h(Lc/zh2;)Ljava/lang/Object;",
        "F_UNIT_INSTANCE" to "Lc/pl7;->a:Lc/pl7;",
        "F_CIDER_RUNTIME_INSTANCE" to "Lc/w32;->n:Lc/w32;",
        "M_ENSURE_INITIALIZED" to "Lc/w32;->w(Landroid/content/ContextWrapper;)V",
        "T_SUSPEND_LAMBDA" to "Lc/a27;",
        "T_CONTINUATION" to "Lc/qb1;",
        "T_FUNCTION2" to "Lc/zh2;",
        "T_MUTABLE_PREFS" to "Lc/kk4;",
        "T_USER_TOKEN_PROBE" to "Lc/dp;",
        "N_CREATE" to "p",
        "N_INVOKE_SUSPEND" to "s",
        "N_FUNCTION2_INVOKE" to "m",
    )

    val GOLDEN_DIR = File("../../patch/smali/com/cidercollective/cider/auth/manual")

    /** Returns the APK named by [envVar], skipping the test when it isn't available. */
    fun apk(envVar: String): File {
        val path = System.getenv(envVar)
        Assume.assumeTrue("$envVar not set", !path.isNullOrBlank())
        val file = File(path!!)
        Assume.assumeTrue("$file missing", file.isFile)
        return file
    }
}
