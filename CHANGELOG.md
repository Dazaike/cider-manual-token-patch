# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.2] - 2026-09-30

All changes below land in the `release: v1.0.2` commit; there were no
intermediate commits since `v1.0.1`.

### Fixed

- Patching Cider 1.0.97 failed with "Could not find runBlocking$default: 2 candidates
  (expected 1)". R8 now emits two identical one-line bridges (`o89.c`, `w89.g`) that both
  call the real `runBlocking`; `SymbolResolver` now picks the bridge in the same class as
  the real method. Verified by resolving every symbol from a Cider 1.0.97 APK.

## [1.0.1] - 2026-09-28

All changes below land in the `release: v1.0.1` commit; there were no
intermediate commits since `v1.0.0`.

### Added

- **Cider Patcher** (`patcher/`), an Android app that applies the manual
  Music-User-Token patch on the phone itself, with no PC:
  - `:core` (Kotlin/JVM) re-derives every R8-obfuscated symbol from the
    input APK's dex files (`SymbolResolver`, 14 anchor rules), renders the
    smali templates in `core/src/main/resources/templates/`, assembles them
    into an extra `classesN.dex` (smali), link-checks every reference
    against the app (`LinkChecker`), adds the sign-in `<activity>` to the
    binary manifest (ARSCLib), and v2/v3-signs the result (apksig). Builds
    whose shape changed fail with a named symbol instead of producing a
    broken APK. Tests: template golden test, resolver tests against
    v1.0.93 and v1.0.80, and an end-to-end patch/verify test.
  - The added activity has no launcher entry, uses a transparent theme,
    takes the token via the `music_user_token` extra, and reports
    `sign_in_result` (`invalid` / `unverified` / `RESULT_OK`) back to the
    caller. Patcher-only template changes are `# BEGIN`/`# END` blocks;
    stripping them reproduces the manual patch exactly.
  - `:app`: patch the installed Cider (or, under **Advanced options**, a
    chosen APK), install via Shizuku or the Android installer (one button,
    chosen in settings), save the APK (advanced), sign in with a token and
    see the outcome as a popup, including a dedicated "couldn't check"
    explanation when Cider isn't signed in to its Cider/Taproom account.
  - **Open Cider** button once the patched Cider is installed; **Revert to
    original** reinstalls the unpatched APK saved at install time so
    Cider's own updates can install again.
  - Shizuku handling: permission requests that close without a result are
    settled on resume; a red error when Shizuku isn't running or stops
    mid-install.
  - Optional **Saved keys** card: Cider license key and Music-User-Token
    stored encrypted with an Android Keystore key, one-tap copy (flagged
    sensitive), and **Use saved token** in the sign-in card.
  - Animated step-by-step patching progress, inline sign-in spinner,
    squircle shapes throughout, and a new app icon.
- `THIRD_PARTY_NOTICES.md`: license texts and copyright notices for every
  open-source library bundled into Cider Patcher (BSD-3-Clause, MIT,
  Bouncy Castle, Apache-2.0) and for the Gradle Wrapper; attached to each
  release alongside the APK.

### Changed

- `LICENSE`: the MIT grant now also covers the Cider Patcher app under
  `patcher/`, and states that bundled third-party libraries and the Gradle
  Wrapper keep their own licenses.
- `README.md`: new "On-device patcher" section covering the patcher's
  flow, the Cider account requirement, reverting for updates, and how to
  build and test it.
- `.gitignore`: ignores the patcher's Gradle build output and
  `local.properties`.

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
