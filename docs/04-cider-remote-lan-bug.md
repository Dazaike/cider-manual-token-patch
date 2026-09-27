# Unrelated finding: "Cider Remote" LAN pairing intermittently fails on Windows

Found while testing the Android patch above (pairing the phone as a
remote for the desktop app). Not related to the Android sign-in patch at
all — documenting it here because the root-cause chain was non-obvious
and the fix is a genuine two-part issue in Cider's own desktop code +
the OS firewall, not anything wrong with the phone/APK.

## Symptom

Phone shows "Paired" after scanning the QR code, but the desktop then
shows: *"Paired, but win32 isn't answering yet. Restart Cider on that
computer to finish setting up LAN access, then try again."* Restarting
Cider does not reliably fix it.

## Cider desktop architecture (relevant part)

Cider's Windows build is a WPF/WebView2 host (`Cider.exe`, closed-source
.NET) that spawns a **bundled Node.js runtime** as a child process
(`NodeRuntime\CiderNode.exe NodeRuntime\index.js`, esbuild-bundled). That
Node process — internally logging under the tag `izanami` — runs a
Fastify HTTP/WebSocket server that both serves the app's own local
SPA/UI *and* handles the LAN-remote API that a paired phone talks to.
Confirmed by:

- Decompiling `Cider.dll` with `ilspycmd` — the WPF/WebView2/tray-icon/IPC
  code (`C2Windows.*` namespace) contains **no** remote/tunnel/websocket
  classes at all.
- Running the bundled `CiderNode.exe`/`index.js` standalone with no
  special environment: it logs a clean boot sequence
  (`[@ciderapp/izanami][boot] ...` → per-module `Module loaded` lines →
  `[@ciderapp/izanami][Server] Server started {"port":10767,...}` →
  `IZANAMI_READY`) and binds port `10767` successfully every time it was
  run this way.

## Root cause 1: an intermittent, completely silent bind race

Reading the bundled `index.js`'s HTTP-server-startup code directly (it's
minified but not obfuscated — plain esbuild output, fully readable
strings/structure):

- Route registration and the actual `server.listen(...)` call only
  happen inside the `.then()` continuation of an internal
  `loadModules()` promise, which sequentially `await`s every bundled
  module's own `setup()` function.
- Each module's `setup()` is individually wrapped in
  `try { ... } catch (e) { console.error("Error loading module", ...) }`
  — but that only catches **synchronous throws or rejected promises**.
  If any one module's `setup()` returns a promise that simply never
  resolves (hangs), the whole `for`/`await` loop stalls there forever,
  and the HTTP server (further down the same promise chain) never
  starts — with **zero error logged anywhere**, because nothing ever
  throws.
- Separately, the listen-retry logic itself (5 retries with backoff,
  falling back from dual-stack to IPv4 on certain errors) only logs via
  plain `console.warn`/`console.log`. As a child of a GUI process with no
  allocated console, that output goes nowhere — so even a *failed* bind
  attempt is invisible.
- Net effect: across several restarts of the real (non-standalone)
  Cider.exe + CiderNode.exe pair, the port was observed bound in three
  different states with no visible cause: **not bound at all**, bound to
  **`127.0.0.1` only** (loopback — reachable from the desktop's own
  WebView2 process, unreachable from a phone on the LAN), and bound to
  **`0.0.0.0`** (all interfaces, correctly LAN-reachable). Which of the
  three you get appears to depend on boot-time timing, not anything the
  user does differently between restarts.

This is a bug in Cider's own bundled Node server, not anything
configurable from the app's settings UI. The only workaround found:
**restart Cider and check `netstat` for the actual bind address** (see
below) — if it's not `0.0.0.0:10767`, keep restarting until it is.

## Root cause 2: no Windows Firewall rule for the app at all

Even once the server is correctly bound to `0.0.0.0:10767`, a phone on
the same LAN still could not reach it. `Get-NetFirewallRule` showed
**zero rules** — not an explicit block, just no rule in either
direction, for either `Cider.exe` or `CiderNode.exe`, on any profile.

On Windows, binding a listening socket never requires firewall
permission — only *accepting an inbound connection from another host*
does. That's why:

- The desktop's own WebView2 process could always reach the server
  (loopback traffic isn't filtered by the normal inbound-rule set).
- The phone's initial "pairing" step could still succeed (it appears to
  go through a token-registration path, not a direct inbound LAN
  connection requiring a firewall allow-rule) — the failure only shows
  up on the *next* step, when the phone actually tries to open a
  persistent connection to the desktop over the LAN.
- Every restart still failed to fix connectivity even once the port was
  correctly bound, because the firewall had nothing to do with restarts
  at all.

Windows normally prompts to create a firewall rule the first time an app
tries to accept an inbound connection; that prompt apparently never
fired (or was dismissed) for this app on this machine, and Cider doesn't
ship/register a firewall rule itself on install.

### Fix

```powershell
# Run as Administrator
New-NetFirewallRule -DisplayName "Cider Remote (10767)" -Direction Inbound -Protocol TCP -LocalPort 10767 -Action Allow -Profile Private
```

Adjust `-Profile` to match whatever `Get-NetConnectionProfile` reports
for your network adapter (`Private`/`Public`/`Domain`).

### How to check current state yourself

```powershell
# Is anything listening on 10767, and on which address?
netstat -ano | findstr :10767
#   0.0.0.0:10767     ...  LISTENING   <pid>   <- good, LAN-reachable
#   127.0.0.1:10767   ...  LISTENING   <pid>   <- bad, loopback-only, restart Cider
#   (nothing at all)                           <- bad, bind race lost this boot, restart Cider

# Is there a firewall rule allowing inbound traffic to it?
Get-NetFirewallRule -DisplayName "*Cider*"
```

## Suggested upstream fix (not filed by this repo's author, documenting for whoever wants to)

- Log the module-`setup()` await loop's *current* module (e.g.
  `console.log`/internal logger before/after each `await setup()`,
  keyed by module id) so a hang shows up as "stuck on module X" instead
  of a silent, indistinguishable freeze.
- Add a per-module `setup()` timeout (`Promise.race` against a
  reasonable deadline) so one broken module can't block the entire
  server from ever starting.
- Register a Windows Firewall inbound rule for the LAN-remote port at
  install time (most desktop apps that need inbound LAN access do this
  via their installer, e.g. an NSIS/WiX custom action calling
  `netsh advfirewall firewall add rule ...`), instead of relying on the
  OS's first-run prompt (which can be dismissed, silently skipped, or
  simply never shown depending on network profile).
