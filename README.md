# Cider Android — Manual Music-User-Token Sign-In Patch

Cider ([ciderapp/Cider-2](https://github.com/ciderapp/Cider-2)) is an open-source
Apple Music client. Its Android build's native "Sign in with Apple" flow goes
through an in-app WebView OAuth handshake
(`com.cidercollective.cider.auth.apple.AppleAuthorizeController` /
`AppleAuthorizeHandshake`). On at least one build/device combination that
handshake never completes — the WebView redirect just fails silently — so
there is no way to reach a signed-in state on Android at all, with no
in-app workaround.

Cider's own **desktop** build already ships a fallback for exactly this
situation: `applyManualToken(token)` in the frontend bundle lets you paste a
`media-user-token` value directly and calls `finishLogin(token)`, bypassing
the popup entirely. The Android app has no equivalent entry point.

This repo is a **binary (smali) patch** that adds one to the Android APK:
a small dialog Activity where you can paste a `media-user-token` value
(the same cookie/localStorage value the desktop fallback already accepts)
and get signed in without touching the broken WebView flow.

It also documents the reverse-engineering process (useful if Cider ships a
new version and the patch needs to be re-derived), and a separate,
unrelated bug found on the desktop side while testing ("Cider Remote" LAN
pairing).

**This is not an official Cider project. Not affiliated with, endorsed by,
or supported by the Cider team.** Use at your own risk; you are
re-signing the APK with your own key, which means Play Protect / Play
Store auto-updates for the *original* signed app will no longer apply to
your patched copy.

## What's in this repo

- [`docs/01-apple-oauth-webview-bug.md`](docs/01-apple-oauth-webview-bug.md) — the problem this patch works around.
- [`docs/02-manual-token-patch-design.md`](docs/02-manual-token-patch-design.md) — patch design, what it calls into, why it's purely additive.
- [`docs/03-r8-symbol-mapping.md`](docs/03-r8-symbol-mapping.md) — how to re-derive the obfuscated symbol table when Cider updates and R8 renames everything.
- [`docs/04-cider-remote-lan-bug.md`](docs/04-cider-remote-lan-bug.md) — unrelated desktop bug: "Cider Remote" LAN pairing intermittently fails; root cause and fix.
- [`patch/smali/`](patch/smali/) — the 10 new smali files, ready to drop into a decoded APK.
- [`patch/AndroidManifest.patch`](patch/AndroidManifest.patch) — the one manifest change needed.
- [`patch/apply.md`](patch/apply.md) — step-by-step apply instructions (apktool → smali drop-in → sign → install).

## Quick summary of the patch

- Adds `com.cidercollective.cider.auth.manual.ManualAppleTokenActivity`, a
  plain `android.app.Activity` (not Compose, not AppCompat — zero theme
  dependency) that shows an `AlertDialog` with an `EditText` for the token.
- On submit, it runs a small hand-written Kotlin-coroutine-shaped state
  machine (`SignInCoroutine`) on a background thread that:
  1. Calls the app's own **existing, unmodified** `AppleMusicApi.probeMusicUserToken(...)` to validate the pasted token against Apple's servers.
  2. If valid: encrypts it with the app's own `SecureTokenStore.encrypt(...)` and writes it to the app's own DataStore under the app's own `KEY_MUSIC_USER_TOKEN` key — the exact same storage the native flow would have used.
  3. Calls the app's own `AppleMusicApi.getUserStorefront(...)` and persists the storefront the same way (best-effort, non-blocking on failure).
  4. Calls the app's own `AppSettings.markOnboardingCompleted()`.
  5. Restarts the app process so every screen picks up the freshly-written state.
- **No existing method body is modified anywhere in the APK.** The patch is
  10 new smali files plus one new `<activity>` manifest entry. It only
  calls pre-existing public methods/fields that the app itself already
  uses for exactly this purpose (token storage, token validation,
  onboarding-completed flag) — it does not reimplement or bypass any of
  Cider's own logic.

Confirmed working end-to-end on a real device: pasted token → validated →
persisted → app restarts signed in with real library/catalog content
loading.

## Disclaimer

Reverse-engineered for personal interoperability/bug-workaround purposes
only (getting a legitimately-purchased/licensed client to reach a
state its own desktop build already supports). No Cider source code,
assets, or compiled binaries are redistributed here — only newly-written
smali files and manifest instructions that you apply to your own,
legitimately-obtained copy of the APK.
