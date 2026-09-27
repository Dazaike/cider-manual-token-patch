# Re-deriving symbols after an R8 obfuscation change

Cider Android auto-updates. Partway through this project the app updated
itself to a build with a **much heavier R8 pass** — the earlier build had
unobfuscated app-package class/method names (`AppleMusicApi`,
`SecureTokenStore`, `CiderRuntime`, ...); the new build renamed
*everything*, including previously-untouched Kotlin-stdlib and AndroidX
classes, down to a single package (`c/`) with single/double-letter class
and method names.

None of the patch's logic changed — every call target is conceptually
the same. Only the *names* changed. This doc is the process used to
re-derive the mapping, so it can be repeated for a future update without
starting from zero.

## Process

1. **Anchor on strings that can't be renamed.** SharedPreferences/DataStore
   key strings, log tag strings, and other string literals survive R8
   renaming untouched (R8 renames identifiers, not string constants).
   Searching the new dex for the exact key names the app persists under
   (e.g. the literal string used as a DataStore preference key, or a
   distinctive log message the app emits on a specific failure path)
   reliably finds the right class even after full renaming.

2. **Anchor on structural cross-references from still-named components.**
   Manifest-declared components (`Activity`/`Service` classes named in
   `AndroidManifest.xml`) are never renamed, because the manifest
   references them by fully-qualified name and R8 must keep that
   resolvable. Any of *their* method bodies that reference the class you
   care about give you a starting cross-reference into the renamed
   graph, even when the class itself has no readable name left.

3. **Confirm interface-implementer shape empirically, not by assumption.**
   For anything implementing a well-known interface/base class from the
   Kotlin runtime or AndroidX (a suspend lambda, a `Function2`, a
   `Preferences.Key`, etc.), find **multiple independent real classes in
   the current build** that already implement the same shape, and check
   they agree with each other on method name/parameter order before
   trusting that shape for your own new code. Don't reuse a mapping
   derived from a previous build's dex — R8's renaming isn't required to
   be stable release-to-release (see the `create`→single-letter,
   parameter-order-swap case in
   [`docs/02-manual-token-patch-design.md`](02-manual-token-patch-design.md)).

## Example mapping table (one specific update, for illustration)

This is the mapping actually used to port this patch from the earlier
(largely unobfuscated) build to the later, fully-obfuscated build. It
will **not** apply as-is to any other build — it's included to show the
*shape* of what changes and what a from-scratch re-derivation has to
produce.

| Readable name | Obfuscated name |
|---|---|
| `CiderRuntime` | `Lc/w32;` |
| `CiderRuntime.INSTANCE` | `Lc/w32;->n:Lc/w32;` |
| `ensureInitialized(ContextWrapper)V` | `Lc/w32;->w(Landroid/content/ContextWrapper;)V` |
| `AppleMusicApi` | `Lc/cs;` |
| `AppleMusicApi.INSTANCE` | `Lc/cs;->a:Lc/cs;` |
| `probeMusicUserToken(String,ContinuationImpl)Enum` | `Lc/cs;->t0(Ljava/lang/String;Lc/rb1;)Ljava/lang/Enum;` |
| `getUserStorefront(ContinuationImpl)Object` | `Lc/cs;->n0(Lc/rb1;)Ljava/lang/Object;` |
| `AppleMusicApi$UserTokenProbe` | `Lc/dp;` |
| `UserTokenProbe.VALID` | `Lc/dp;->l` |
| `SecureTokenStore` | `Lc/aa6;` |
| `encrypt(String)String` | `Lc/aa6;->c(Ljava/lang/String;)Ljava/lang/String;` |
| `dataStore` | `Lc/aa6;->i:Lc/yf1;` |
| `KEY_MUSIC_USER_TOKEN` | `Lc/aa6;->a:Lc/fi5;` |
| `KEY_STOREFRONT` | `Lc/aa6;->b:Lc/fi5;` |
| `AppSettings` | `Lc/mn;` |
| `markOnboardingCompleted()V` | `Lc/mn;->s()V` |
| `kotlin.coroutines.jvm.internal.SuspendLambda` | `Lc/a27;` |
| `kotlin.coroutines.jvm.internal.ContinuationImpl` | `Lc/rb1;` |
| `kotlin.coroutines.Continuation` | `Lc/qb1;` |
| `kotlin.jvm.functions.Function2` (method `invoke`) | `Lc/zh2;` (method `m`) |
| `invokeSuspend` | `s` |
| `CoroutineSingletons` / `COROUTINE_SUSPENDED` | `Lc/bd1;` / field `l` |
| `kotlin.ResultKt.throwOnFailure` | `Lc/g38;->i(Ljava/lang/Object;)V` |
| `kotlin.Unit` / `INSTANCE` | `Lc/pl7;` / field `a` |
| `androidx.datastore.preferences.core.MutablePreferences.set` | `Lc/kk4;->e(Lc/fi5;Ljava/lang/Object;)V` |
| `androidx.datastore.preferences.core.Preferences$Key` | `Lc/fi5;` |
| `androidx.datastore.core.DataStore` | `Lc/yf1;` |
| `androidx.datastore.preferences.core.PreferencesKt.edit` | `Lc/oa0;->f(Lc/yf1;Lc/zh2;Lc/qb1;)Ljava/lang/Object;` |
| `kotlinx.coroutines.BuildersKt.runBlocking` (R8-merged into `AwaitKt`) | `Lc/pe8;->h(Lc/zh2;)Ljava/lang/Object;` |
| `SuspendLambda.create(Object,Continuation)` | renamed to `p`, **and parameter order swapped** to `(Continuation, Object)` — see note in doc 02 |

The smali in [`patch/smali/`](../patch/smali/) targets this specific
mapping. If your build's dex doesn't match these method signatures,
you'll need to redo steps 1–3 above before the patch will verify/dex.
