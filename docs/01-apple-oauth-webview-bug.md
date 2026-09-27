# The bug: native Apple sign-in never completes on Android

Cider Android's sign-in flow is a WebView-hosted OAuth handshake:

- `com.cidercollective.cider.auth.apple.AppleAuthorizeController` drives the flow.
- `AppleAuthorizeHandshake` performs the actual redirect/token exchange with
  Apple's auth servers inside an embedded WebView.

On the device this was tested on, that handshake never reaches a
completed state — the WebView redirect chain stalls/fails silently, with
no error surfaced to the user beyond staying on the sign-in screen
indefinitely. No combination of retrying, clearing app data, or
reinstalling changed the outcome.

Cider's desktop build (Electron/WebView2 + Vue frontend) hits the same
class of WebView-OAuth fragility often enough that it already ships a
deliberate escape hatch: a manual-token entry path
(`applyManualToken(token)` in the frontend bundle) that accepts a raw
`media-user-token` value pasted by the user and calls `finishLogin(token)`
directly, skipping the WebView popup flow entirely. This is a supported,
intentional fallback in the desktop app — not a jailbreak of it.

The Android app ships no equivalent. There is no way to reach this
fallback path without either (a) Cider adding it upstream, or (b) adding
it yourself via a binary patch, which is what this repo does.

## Why a manual-token entry is a legitimate workaround, not a bypass

The `media-user-token` is the same artifact the *working* WebView flow
would have obtained and stored anyway — it's Apple Music's own bearer
token for an already-authenticated `music.apple.com` session, obtainable
by the user from their own browser's cookies/localStorage (or from
Cider desktop's own already-authenticated WebView profile) after signing
in through channels that do work. This patch does not defeat any
authentication check; it just gives the Android app another way to
receive a token it would otherwise have gotten via the (broken) in-app
browser.
