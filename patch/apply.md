# Applying the patch

Requires: `apktool`, Android SDK build-tools (`zipalign`, `apksigner`),
`adb`, and a JDK — all standard Android reverse-engineering tooling, no
Cider-specific dependencies.

These steps assume the symbol mapping in
[`../docs/03-r8-symbol-mapping.md`](../docs/03-r8-symbol-mapping.md)
still matches the APK you have. **Check this first** — if Cider has
updated since this mapping was derived, the smali as-is will fail to
verify/dex, and you'll need to re-derive the mapping yourself (process
described in that doc) and edit the smali's method/field references
accordingly before continuing.

1. **Decode.**
   ```
   java -jar apktool.jar d cider.apk -o cider_src -f
   ```

2. **Drop in the new classes.** Copy everything under
   [`smali/com/cidercollective/cider/auth/manual/`](smali/com/cidercollective/cider/auth/manual/)
   into `cider_src/smali/com/cidercollective/cider/auth/manual/` (create
   the directory).

3. **Patch the manifest.** Apply
   [`AndroidManifest.patch`](AndroidManifest.patch) to
   `cider_src/AndroidManifest.xml` (it's a one-block insertion, easiest
   to just paste the `<activity>` block in by hand next to the app's
   existing `MainActivity` entry).

4. **Rebuild.**
   ```
   java -jar apktool.jar b cider_src -o cider_patched.apk
   ```

5. **Zipalign then sign** (order matters):
   ```
   zipalign -p -f 4 cider_patched.apk cider_patched_aligned.apk
   apksigner sign --ks <your-keystore> --ks-pass pass:<pass> --key-pass pass:<pass> ^
       --out cider_patched_signed.apk cider_patched_aligned.apk
   apksigner verify cider_patched_signed.apk
   ```
   Any keystore works — the Android SDK's default debug keystore
   (`~/.android/debug.keystore`, alias `androiddebugkey`, password
   `android` for both) is fine for personal use. You're replacing the
   original developer signature, so this is a fresh identity as far as
   Android is concerned — see the note below about what that means for
   updates.

6. **Install.** The original app is signed with the real developer's
   key; your rebuild is signed with yours. Android will refuse a
   same-package update across different signing certs, so you must
   uninstall the original first:
   ```
   adb uninstall com.cidercollective.cider
   adb install cider_patched_signed.apk
   ```

## Verify

Thanks to the `<intent-filter>` in the manifest patch, the dialog gets
its own icon in the app drawer/home screen — labeled "Cider Manual
Sign-In", using Cider's own launcher icon. Tap it directly, no computer
needed after the initial install.

To confirm it's wired up without tapping anything, from a computer:
```
adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER com.cidercollective.cider
```
should list `.auth.manual.ManualAppleTokenActivity` alongside `.MainActivity`.

Or launch it directly the same way:
```
adb shell am start -n com.cidercollective.cider/.auth.manual.ManualAppleTokenActivity
```
Either way it should bring up the dialog (title "Sign in with
Music-User-Token", an `EditText`, Cancel/Sign in buttons) without a
crash — this alone confirms the manifest entry and new classes loaded
correctly.

Then paste a real `media-user-token` value (get it from your browser's
devtools on an authenticated `music.apple.com` session, or from Cider
desktop's own already-authenticated profile) and tap "Sign in". Expect:
a couple seconds of validation, then the app process restarts landing on
the signed-in home screen with real library/catalog content.

## What this costs you going forward

- **You will not receive automatic updates** for this app through its
  normal update mechanism while running the patched build, because it's
  signed with a different key than the original. To update, pull the new
  version's `base.apk` (e.g. via `adb pull` from
  `/data/app/~~.../base.apk` after letting the *original* signed app
  update itself once), then redo this patch against the new APK.
- Re-derive the symbol mapping (doc 03) any time the underlying app
  updates and this patch's smali fails to build/verify against it — R8's
  renaming is not guaranteed stable release to release.
