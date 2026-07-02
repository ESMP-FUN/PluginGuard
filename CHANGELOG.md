# Changelog

All notable changes to PluginGuard will be documented in this file.

## [1.2.0] - 2026-07-02

- Real Minecraft 26.x support: one codebase now builds two jars, selected with
  `-Pmc=<line>` (mirrors AntiDupePro's release model).
  - `./gradlew shadowJar -Pmc=21` -> `PluginGuard-1.2.0.jar` (compiled against
    Paper API 1.21.11, Java 21 bytecode - for 1.21.x servers).
  - `./gradlew shadowJar -Pmc=26` -> `PluginGuard-1.2.0-mc26.jar` (compiled against
    Paper API 26.1.2, Java 25 bytecode - for 26.x servers).
- New: in-game server-brand spoofing. `PaperServerListPingEvent` only covered the
  server-list ping; the brand shown in the F3 debug screen (and read by "server
  brand" client mods) travels separately in the `minecraft:brand` plugin-message
  channel, sent during the configuration phase before a player fully joins.
  PluginGuard now injects a Netty pipeline handler at the server-channel level
  (TinyProtocol-style, before login) and rewrites that payload to `fake-server-brand`.
  Reuses the existing `hide-server-brand` toggle. Reflection on plain paper-api, no
  ProtocolLib/paperweight; fails open (Spigot's remapped internals degrade to
  ping-only spoofing, as before).
- Hardened the command-preprocess parser: it now skips any whitespace between the
  slash and the command token before matching (`/  plugins`, `/<tab>plugins`), so a
  padded command can't slip past the guard on a dispatcher that tolerates it.
- New: `block-namespaced-commands` (default: on). Blocks every namespaced command
  (`/essentials:home`, `/luckperms:lp`, ...) and strips namespaced entries from tab
  completion - the namespace before the colon is the plugin's name, so a single
  namespaced command confirmed a plugin's presence even with its bare alias hidden.
  Namespaced probes are recorded at high weight (3) by the detector.
- Fixed: `block-bukkit-commands` documented that it blocks `minecraft:`-prefixed
  commands but only ever blocked `bukkit:`. It now blocks both, as documented.
- Fixed: `block-unknown-commands` was read from the config but never enforced.
  It now works: when a player runs a plugin command they lack permission for,
  PluginGuard cancels it and answers with the vanilla unknown-command line before
  the plugin can leak its existence through a "You don't have permission" reply.
  (Applies to commands that declare a permission; not tracked by the detector,
  since legitimate players hit permission walls routinely.)
- Fixed: probe-detector alert details (recent labels, window age) are now
  snapshotted inside the tracker lock, closing a small race where a concurrent
  probe from the same player could skew the alert message.
- `/pluginguard` now tab-completes its subcommands (`reload`, `status`).
- `/version` spoof no longer wraps the fake brand in literal escaped quotes.
- Build: Kotlin 2.3.20; plain (non-shaded) jar task disabled - only the shaded
  jar is ever released.

## [1.1.0] - 2026-06-01

- Probe logging and pattern detection. PluginGuard now records when players try
  to enumerate the plugin list and can alert online admins when a player crosses
  a configurable weighted-score threshold within a sliding window.
  - Weighted categories: high (3) for `bukkit:`/`minecraft:` prefixes and
    `/icanhasbukkit`; medium (2) for `/pl`, `/plugins`, `/ver`, `/version`,
    `/about`; low (1) for common-plugin enumeration (`/lp`, `/we`, `/co`, ...).
  - `/help` and `/?` are deliberately never tracked.
  - Optional append-only `plugins/PluginGuard/probes.log` audit trail.
  - In-game alerts gated by new `pluginguard.alerts` permission (default: op).
  - All I/O dispatched via Paper/Folia's async and global-region schedulers, so
    the event hot path stays cheap and Folia-safe.
  - Per-player state lives in a `ConcurrentHashMap` and is cleared on quit.
- Honeypot commands. Admins can list fake commands in `honeypot-commands:` that
  no legitimate user would ever type — a single honeypot hit weighs 5, so by
  default it triggers an alert immediately.
- `/pluginguard status` now reports detection state, honeypot count, and file-log state.

## [1.0.0] - 2026-06-01

- Target Paper API 1.21.4; supports Minecraft 1.21.x (Java 21+) and 26.x.x (Java 25+).
- Folia support declared via `folia-supported: true`.
- Graceful degradation on Spigot/Bukkit: the Paper-only `PaperServerListPingEvent`
  listener is now registered conditionally via class-presence check, so the plugin
  loads on non-Paper servers (server-brand spoofing simply becomes a no-op).
- All player-facing messages now use the Adventure `Component` API instead of the
  deprecated legacy color-code strings.
- `/version` spoof now reflects the running server's Minecraft version dynamically
  instead of a hard-coded `1.21.1`.
- Build: Kotlin 2.1.20, Shadow 8.3.6, jar version is now wired through `plugin.yml`
  via `${version}`.
- Thread-safe, lock-free reload: all config-derived state is held in an immutable
  `Settings` snapshot behind a `@Volatile` reference, so `/pluginguard reload` swaps
  the whole snapshot atomically and concurrent event handlers on different Folia
  region threads always observe a consistent set of values without taking any lock.
- Hot-path allocation reduced: `PlayerCommandPreprocessEvent` no longer
  lowercases the entire message or splits on spaces — only the base command token
  is sliced out and lowercased. Saves work on every command a player types,
  proportional to message length.
- README rewritten as proper UTF-8 (was UTF-16 with BOM).

## [0.0.1] - 2025-11-01

- Initial release.
