# Patch design

## Constraints that shaped the design

- **Purely additive.** No existing method body in the APK is touched —
  only new classes plus one new `<activity>` manifest entry. This
  minimizes the chance of breaking anything in an R8-optimized build
  where a "small" edit to an existing method can have non-obvious
  ripple effects (inlined call sites, merged classes, etc.).
- **Reuse the app's own storage/validation code, don't reinvent it.**
  The app already has `AppleMusicApi.probeMusicUserToken(...)` (validates
  a token against Apple), `SecureTokenStore` (Tink-AEAD-encrypted
  AndroidX DataStore-backed token storage), and
  `AppSettings.markOnboardingCompleted()`. All three are `public` and
  callable from new code without modification. The patch calls into
  these directly instead of reimplementing encryption or storage.
- **No UI framework dependency.** The app's real screens are Jetpack
  Compose. Rather than trying to inject a new Compose screen into an
  R8-optimized Compose call graph, the patch uses a plain
  `android.app.Activity` + `AlertDialog.Builder`, which has no
  dependency on the app's Compose/AppCompat theme setup at all.

## Component list

| File | Role |
|---|---|
| `ManualAppleTokenActivity` | Entry point (manifest-declared, exported). Builds the `EditText` + `AlertDialog` in `onCreate`. |
| `ManualAppleTokenActivity$OnShowListener` | Overrides the positive button's default dismiss-on-click behavior so it can validate input first (`AlertDialog.setOnShowListener` + `Button.setOnClickListener`, the standard Android pattern for "don't auto-dismiss on invalid input"). |
| `ManualAppleTokenActivity$PositiveClickListener` | Validates non-empty input; if empty, shows a `Toast` and leaves the dialog open; otherwise dismisses the dialog and starts `SignInThread`. |
| `ManualAppleTokenActivity$NegativeClickListener` | Cancel button → `Activity.finish()`. |
| `ManualAppleTokenActivity$SignInThread` | A plain `Thread` that runs `SignInCoroutine` via `runBlocking`-equivalent (`AwaitKt.runBlocking$default` / renamed equivalent — see below), off the UI thread. |
| `SignInCoroutine` | The actual sign-in state machine (see below). |
| `SignInCoroutine$InvalidTokenRunnable` | UI-thread `Runnable`: shows the "token isn't valid" `Toast` and finishes the activity. |
| `SignInCoroutine$FinishRunnable` | UI-thread `Runnable`: relaunches the app process and exits, so every screen reads the freshly-persisted signed-in state. |
| `SaveTokenEdit`, `SaveStorefrontEdit` | Tiny `Function2` implementations for `PreferencesKt.edit(...)`/its renamed equivalent — each just does `MutablePreferences.set(KEY, encryptedValue)` for one DataStore key. |

## `SignInCoroutine` state machine

Written by hand as a `SuspendLambda` subclass (the same shape the Kotlin
compiler itself generates for a suspend lambda) because there's no
Kotlin compiler in the smali-patching loop — this is directly authored
smali replicating that shape. It's a manual label-driven state machine,
one `label` value per suspension point:

```
label 0: probeMusicUserToken(token) — suspend, may return COROUTINE_SUSPENDED
label 1: check probe result == VALID
           if not valid  -> Toast + finish(), done
           if valid      -> encrypt(token) -> DataStore.edit(SaveTokenEdit) — suspend
label 2: getUserStorefront() — suspend
label 3: if result is a non-empty String -> encrypt(storefront) -> DataStore.edit(SaveStorefrontEdit) — suspend
           else -> skip straight to finish step (storefront failure is non-blocking)
label 4 / finish step: AppSettings.markOnboardingCompleted()
           -> relaunch app process via getLaunchIntentForPackage + FLAG_ACTIVITY_NEW_TASK|FLAG_ACTIVITY_CLEAR_TASK
           -> Runtime.getRuntime().exit(0)
```

Runs on a background `Thread` via a blocking coroutine runner (not
`GlobalScope.launch`) — deliberately, to avoid having to wire up a
`CoroutineScope`/`Dispatchers.IO` from scratch in smali.

## Two subtle bugs found and fixed during real-device testing

These are worth reading even if you're not touching Cider specifically —
they're general lessons about hand-writing coroutine-shaped smali against
an R8-optimized build.

### 1. Back button bypassed `setCancelable(false)`

`AlertDialog.setCancelable(false)` only prevents the **dialog** from being
dismissed by back-press/outside-tap — it says nothing about the
**Activity** hosting it. The Activity's default `onBackPressed()` still
calls `finish()`, which tears down the whole screen (dialog included) on
back-press, silently defeating the "must submit a token" intent.

Fix: override `onBackPressed()` with an empty body (consumes the event,
does nothing) directly on `ManualAppleTokenActivity`.

### 2. `SuspendLambda.create`'s name *and parameter order* are not stable across R8 builds

In one build of the app, the Kotlin-compiler-emitted override of
`create(Object, Continuation)` was present with its original name and
source parameter order. After the app updated to a new version with a
heavier R8 pass, the equivalent override:

- was renamed (not to `create` — to a single letter, different in every
  R8 run),
- **and had its two parameters silently swapped** to
  `(Continuation, Object)` instead of the source order
  `(Object, Continuation)`.

Because R8 renaming is consistent *within* one build (every real call
site and override agrees), but not necessarily consistent *across*
builds, an override written against the previous build's name+order
compiles and installs fine — but at runtime, dex method dispatch falls
through to the abstract base-class stub, which unconditionally throws
`UnsupportedOperationException: create(...) has not been overridden`.

The only reliable fix is empirical, not something you can assume from a
previous mapping: find **two real, unrelated classes** in the *current*
build's dex that already extend the same base class and implement the
same interface (i.e., are already known-good examples of this exact
override), and match their override's name and parameter order exactly.
Do not reuse a previous build's mapping for this override without
re-verifying it against real examples in the new build.

See [`docs/03-r8-symbol-mapping.md`](03-r8-symbol-mapping.md) for the
general process this came out of.
