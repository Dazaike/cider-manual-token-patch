# Changelog

Format: [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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
