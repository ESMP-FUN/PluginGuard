# Changelog

All notable changes to PluginGuard will be documented in this file.

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
