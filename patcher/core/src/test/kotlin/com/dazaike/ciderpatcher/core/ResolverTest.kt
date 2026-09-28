package com.dazaike.ciderpatcher.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

private fun resolve(apk: File): SymbolMap = ZipFile(apk).use { zip ->
    SymbolResolver(DexIndex(DexIndex.parse(DexIndex.readDexEntries(zip)))).resolve()
}

class ResolverV93Test {
    @Test
    fun resolvesV93Names() {
        val map = resolve(TestFixtures.apk("CIDER_APK_93"))
        assertEquals(SymbolMap.withDerived(TestFixtures.V93, CreateOrder.CONT_FIRST), map)
    }
}

class ResolverV80Test {
    @Test
    fun resolvesV80ReadableNames() {
        val map = resolve(TestFixtures.apk("CIDER_APK_80"))
        assertEquals(CreateOrder.VALUE_FIRST, map.createOrder)
        assertEquals(SymbolMap.withDerived(TestFixtures.V93, CreateOrder.CONT_FIRST).values.keys, map.values.keys)
        val expected = mapOf(
            "N_CREATE" to "create",
            "N_INVOKE_SUSPEND" to "invokeSuspend",
            "N_FUNCTION2_INVOKE" to "invoke",
            "T_SUSPEND_LAMBDA" to "Lkotlin/coroutines/jvm/internal/SuspendLambda;",
            "M_PROBE_MUSIC_USER_TOKEN" to "Lcom/cidercollective/cider/api/AppleMusicApi;->probeMusicUserToken(Ljava/lang/String;Lkotlin/coroutines/jvm/internal/ContinuationImpl;)Ljava/lang/Enum;",
            "F_CIDER_RUNTIME_INSTANCE" to "Lcom/cidercollective/cider/CiderRuntime;->INSTANCE:Lcom/cidercollective/cider/CiderRuntime;",
            "M_RUN_BLOCKING_DEFAULT" to "Lkotlinx/coroutines/AwaitKt;->runBlocking\$default(Lkotlin/jvm/functions/Function2;)Ljava/lang/Object;",
            "M_MARK_ONBOARDING_COMPLETED" to "Lcom/cidercollective/cider/api/AppSettings;->markOnboardingCompleted()V",
            "M_PREFS_SET" to "Landroidx/datastore/preferences/core/MutablePreferences;->set(Landroidx/datastore/preferences/core/Preferences\$Key;Ljava/lang/Object;)V",
            "F_PROBE_INVALID" to "Lcom/cidercollective/cider/api/AppleMusicApi\$UserTokenProbe;->INVALID:Lcom/cidercollective/cider/api/AppleMusicApi\$UserTokenProbe;",
            "F_KEY_MUSIC_USER_TOKEN" to "Lcom/cidercollective/cider/api/SecureTokenStore;->KEY_MUSIC_USER_TOKEN:Landroidx/datastore/preferences/core/Preferences\$Key;",
        )
        for ((key, value) in expected) assertEquals(key, value, map.values[key])
    }
}
