# Cider Android — Manual Music-User-Token Sign-In Patch

[![Release](https://img.shields.io/github/v/release/Dazaike/cider-manual-token-patch)](https://github.com/Dazaike/cider-manual-token-patch/releases/latest) [![Changelog](https://img.shields.io/badge/changelog-CHANGELOG.md-blue)](CHANGELOG.md)

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
- [`patcher/`](patcher/) — **Cider Patcher**, an Android app that applies this patch on the phone itself (no PC), re-deriving the obfuscated names from whichever Cider build you give it. See below.
- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) — licenses of the open-source libraries bundled into Cider Patcher, and of the Gradle Wrapper.

## Quick summary of the patch

- Adds `com.cidercollective.cider.auth.manual.ManualAppleTokenActivity`, a
  plain `android.app.Activity` (not Compose, not AppCompat — zero theme
  dependency) that shows an `AlertDialog` with an `EditText` for the token.
  It's registered with a `MAIN`/`LAUNCHER` intent-filter, so it gets its
  own icon in the app drawer/home screen (labeled "Cider Manual
  Sign-In") — no computer/adb needed to launch it after the initial install.
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

## On-device patcher (`patcher/`)

`patcher/` is a Gradle project with two modules:

- `:core` (plain Kotlin/JVM): resolves every R8-obfuscated symbol the patch
  needs from the input APK's dex files using obfuscation-proof anchors
  (const-strings, descriptors, class shapes), renders the smali templates in
  `core/src/main/resources/templates/`, assembles them with smali into an extra
  `classesN.dex`, link-checks every reference against the app, adds the
  sign-in `<activity>` to the binary manifest (ARSCLib), and v2/v3-signs the
  result (apksig). A build whose shape changed fails with a named symbol
  instead of producing a broken APK.
- `:app` (`com.dazaike.ciderpatcher`, "Cider Patcher"): tap **Use installed
  Cider** (needs a non-split install), tap **Patch**, then **Install via
  Shizuku**. **Choose APK**, **Save APK**, and the install method (Shizuku,
  or the Android installer with its confirmation dialogs) are behind
  **Advanced options** in the settings menu (top right); only the chosen
  method's install button is shown. After installing, tap **Open Cider**
  and sign in to your Cider (Taproom) account: Cider needs it to fetch the
  Apple Music developer token, and without it every token check fails.
  Then type your Music-User-Token into the patcher and tap **Sign in to
  Cider**. The patcher starts the patched activity for a result with the
  token in the `music_user_token` extra. The activity signs in with no
  dialog and reports back, and the patcher shows the outcome in a popup:
  signed in, token rejected by Apple, or couldn't check (no Cider account
  sign-in, or offline). The token stays in memory unless you choose to keep
  it: the optional **Saved keys** card stores your Cider license key and
  Music-User-Token encrypted with an Android Keystore key, on this phone
  only, with one-tap copy (marked sensitive so the clipboard preview hides
  it) and a **Use saved token** shortcut in the sign-in card.

Differences from the manual patch in `patch/`: the patcher's activity has no
label, icon, or launcher `intent-filter` (so there's no extra app-drawer
entry); it uses the transparent `Theme.DeviceDefault.Panel`, so nothing
appears while it signs in (the patcher shows a spinner on its Sign in button);
it reads the token extra before falling back to the dialog; and when
started for a result it returns `sign_in_result` (`invalid`/`unverified`, or
`RESULT_OK`) instead of showing a toast. Every patcher-only addition is a
`# BEGIN …`/`# END …` block in the templates, so stripping those blocks
reproduces the manual patch exactly (checked by `TemplateGoldenTest`).

The patcher signs with its own key, generated on first use and kept in the
app's private storage. The first install over a differently signed Cider
needs an uninstall (the app prompts for it, and it wipes Cider's data).

When a patched build installs, the patcher keeps the unpatched APK it was
made from. Cider's own updates are signed by its developers and can't
install over the patched app, so the **Cider updates** card offers **Revert
to original**: it uninstalls the patched Cider and reinstalls that saved
original (wiping Cider's data). Update Cider normally, then patch the new
version.

Build and test from `patcher/` (the APK-backed tests are skipped when the
env vars aren't set):

```
set CIDER_APK_93=C:\path\to\cider-1.0.93.apk
set CIDER_APK_80=C:\path\to\cider-1.0.80-patched.apk
gradlew.bat :core:test :app:assembleDebug
```

Every release that attaches `CiderPatcher-*.apk` also attaches
`THIRD_PARTY_NOTICES.md`, which the bundled libraries' BSD/MIT/Apache
licenses require to accompany the app. Regenerate it when dependencies
change.

## Disclaimer

Reverse-engineered for personal interoperability/bug-workaround purposes
only (getting a legitimately-purchased/licensed client to reach a
state its own desktop build already supports). No Cider source code,
assets, or compiled binaries are redistributed here — only newly-written
smali files and manifest instructions that you apply to your own,
legitimately-obtained copy of the APK.

## License

The newly-authored files in this repository, including the Cider Patcher
app, are MIT-licensed; see [`LICENSE`](LICENSE) for exactly what it covers.
Third-party libraries and the Gradle Wrapper keep their own licenses; see
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
