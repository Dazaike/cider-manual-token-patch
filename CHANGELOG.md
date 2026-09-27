# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.0] - 2026-09-27

Initial release.

### Added

- Manual Music-User-Token sign-in patch for Cider Android
  (`a49761b`): 10 new smali files under
  `patch/smali/com/cidercollective/cider/auth/manual/`, one manifest
  `<activity>` addition (`patch/AndroidManifest.patch`), and step-by-step
  apply instructions (`patch/apply.md`) for working around the broken
  native Apple OAuth WebView sign-in flow on Android.
- Documentation set (`a49761b`):
  - `docs/01-apple-oauth-webview-bug.md` — the original WebView bug this
    patch works around.
  - `docs/02-manual-token-patch-design.md` — patch design, component
    breakdown, and two R8-obfuscation gotchas found during real-device
    testing (back-press bypassing `setCancelable`, and
    `SuspendLambda.create`'s name/parameter order not being stable
    across R8 builds).
  - `docs/03-r8-symbol-mapping.md` — the general process for
    re-deriving obfuscated symbol names after a Cider update, plus the
    symbol table used for this patch's target build.
  - `docs/04-cider-remote-lan-bug.md` — an unrelated desktop-side
    finding: Cider Remote's LAN pairing intermittently fails due to a
    silent module-load race in the bundled Node server plus a missing
    Windows Firewall rule; root causes and fixes.
- MIT license for the newly-authored patch/docs content (`a49761b`),
  with a NOTICE that it grants no rights to Cider itself.
- Launcher shortcut for the sign-in dialog (`22c4d8e`): the patched
  activity now declares a `MAIN`/`LAUNCHER` intent-filter (label "Cider
  Manual Sign-In", using Cider's own icon), so it can be launched with a
  tap from the home screen/app drawer instead of requiring `adb shell am
  start` from a computer.
