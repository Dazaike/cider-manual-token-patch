# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.3] - 2026-10-12

### Added

- **Sid (cloud Gemini)**: on Cider builds that have Sid (1.0.129+), the patcher hooks Sid so it can run on Gemini over a Google AI Studio API key instead of on-device Gemini Nano. Sid then works on phones without Nano. The key and model id are entered in the new **Sid** sheet and sent to the patched Cider; with no key, Sid behaves as stock.
- New **Hooking Sid to the cloud** patch step. Builds without Sid, or where Sid's internals can't be matched, skip it and still get the token patch ("Sid cloud: skipped" is shown).
- The key is stored in plaintext in the patched Cider's private storage; what you ask Sid is sent to Google.
- **Open Cider after install** setting (off by default): after a Shizuku install, copies your saved license key (marked sensitive) and opens Cider.
- Accent colour and haptics settings.

### Changed

- **Patcher UI fully redesigned** as a guided journey in a liquid-glass dark style: one home screen with a Patch → Install → Sign in progress rail and a single primary action pinned at the bottom. Sid, Saved keys, Revert and Settings are secondary sheets and rows. Status messages are now toasts. Material 3 is gone from the app.
- **One-tap patching**: "Patch Cider" copies and patches the installed Cider in one step. "Choose APK file" is offered on the home screen when Cider isn't installed, and otherwise stays under Advanced options.
- After a successful install the home screen moves to the Sign in step; a successful sign-in shows "You're signed in".
- Build toolchain updated to Gradle 9.3.1, AGP 9.1.1, Kotlin 2.4.20, Compose 1.12.1.

## [1.0.2] - 2026-09-30

### Fixed

- Patching Cider beta 3 failed with "Could not find runBlocking$default: 2 candidates". The resolver now picks the bridge beside the real `runBlocking`.

## [1.0.1] - 2026-09-28

### Added

- **Cider Patcher** (`patcher/`): an Android app that applies the manual token patch on the phone, no PC needed. It finds the obfuscated names in any Cider build, patches and signs the APK, and installs via Shizuku or the normal Android installer. Builds it can't match fail with the missing symbol named.
- Sign-in with a Music-User-Token, **Open Cider**, and **Revert to original** so Cider's own updates can install again.
- Optional **Saved keys** card: Cider license key and token stored encrypted.
- `THIRD_PARTY_NOTICES.md`: licenses for bundled libraries, attached to each release.

### Changed

- `LICENSE` now also covers the patcher app.
- `README.md` gains an "On-device patcher" section.

## [1.0.0] - 2026-09-27

Initial release.

### Added

- Manual Music-User-Token sign-in patch for Cider Android (`a49761b`): smali files, manifest change, and apply instructions, working around the broken Apple OAuth WebView sign-in.
- Docs on the WebView bug, patch design, R8 symbol mapping, and a Cider Remote LAN pairing bug.
- MIT license for the patch and docs (`a49761b`).
- Launcher shortcut for the sign-in dialog (`22c4d8e`).
