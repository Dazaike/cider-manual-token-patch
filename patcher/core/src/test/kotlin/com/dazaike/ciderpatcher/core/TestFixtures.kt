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

    /** Cider v1.0.129 R8 names for the Sid cloud hooks (anchors listed in the Sid plan). */
    val V129_SID: Map<String, String> = mapOf(
        "T_KQ7" to "Lc/kq7;",
        "N_KQ7_CAPS" to "l",
        "N_KQ7_WARMUP" to "s",
        "T_CONT_IMPL" to "Lc/gp1;",
        "T_L0C" to "Lc/l0c;",
        "T_TZB" to "Lc/tzb;",
        "N_TZB_STATUS" to "a",
        "N_L0C_STRUCTURED_OK" to "h",
        "N_L0C_GENERATE" to "k",
        "T_I23" to "Lc/i23;",
        "T_KCLASS" to "Lc/q81;",
        "T_MM8" to "Lc/mm8;",
        "T_IN1" to "Lc/in1;",
        "F_I23_TEMPERATURE" to "Lc/i23;->a:F",
        "F_I23_CONTENTS" to "Lc/i23;->f:Ljava/util/List;",
        "F_IN1_PARTS" to "Lc/in1;->a:Ljava/util/List;",
        "F_MM8_TEXT" to "Lc/mm8;->a:Ljava/lang/String;",
        "T_J23" to "Lc/j23;",
        "M_J23_INIT" to "Lc/j23;-><init>(Ljava/util/ArrayList;)V",
        "T_VT0" to "Lc/vt0;",
        "M_VT0_INIT" to "Lc/vt0;-><init>(Ljava/lang/Integer;Ljava/lang/String;)V",
        "T_E23" to "Lc/e23;",
        "M_E23_INIT" to "Lc/e23;-><init>(ILjava/lang/String;Ljava/lang/Throwable;)V",
        "T_TO7" to "Lc/to7;",
        "M_TO7_INIT" to "Lc/to7;-><init>(Ljava/lang/Integer;ZZLjava/lang/Integer;)V",
        "T_MLKIT_CONTEXT" to "Lc/p85;",
        "N_MLKIT_CONTEXT_GET" to "c",
        "N_MLKIT_CONTEXT_APP" to "b",
        "M_SCHEMA_KEY" to "Lc/ubb;->c(Lc/q81;)Lc/v24;",
        "M_SCHEMA_NODE" to "Lc/v24;->i()Lc/owa;",
        "M_SCHEMA_TEXT" to "Lc/yzb;->a(Ljava/lang/String;Lc/owa;)Ljava/lang/String;",
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
