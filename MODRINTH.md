# PluginGuard

**Stop players from fingerprinting your server's plugin list.**

PluginGuard is a lightweight, drop-in protection layer that intercepts the commands and protocol channels players use to discover which plugins you're running — `/plugins`, `/pl`, `/ver`, `bukkit:` and `minecraft:` prefixed commands, tab-completion probing, and server-list pings — and replaces the response with whatever you want them to see.

---

## Why it exists

Knowing which plugins a server runs is the first step in attacking it. A player who knows you run a specific economy plugin, an outdated permissions plugin, or a known-vulnerable utility can target known CVEs or exploit-known quirks. Default Bukkit gives that information away to anyone who types `/pl`.

PluginGuard closes every public surface that leaks plugin presence.

---

## Features

- **Hide Mode** — choose how the server responds to probes:
  - `unknown-command` — indistinguishable from a typo (most realistic)
  - `empty` — `Plugins (0):`
  - `fake-list` — return a list of plausible-looking decoy plugins
  - `permission-denied` — pretend the player lacks permission
- **Plugin spoofing** — return any configurable list of fake plugins. Make a hardened production server look like vanilla Paper with two utility plugins.
- **`bukkit:` / `minecraft:` prefix protection** — block or redirect prefixed-command probing.
- **Tab-completion hardening** — strip plugin commands from `/[tab]` so they can't be discovered by autocomplete.
- **Common-plugin command blocklist** — `/essentials`, `/lp`, `/we`, `/co`, `/mv`, `/dynmap`, `/gp`, and friends all return "Unknown command" instead of "You don't have permission" — denying the existence/permission distinction attackers use to enumerate.
- **Server-brand spoofing** — return `vanilla` (or anything you configure) in MOTD / server-list ping protocol responses.
- **Aggressive mode** — block every plugin command by default; only players with the explicit `<command>.use` permission can use them.
- **Bypass permission** — staff with `pluginguard.bypass` see the real server, untouched.
- **Hot reload** — `/pluginguard reload` swaps the live config atomically.

---

## Compatibility

| Property | Value |
| --- | --- |
| Minecraft | **1.21.x** and **26.x.x** |
| Java | **21+** (1.21.x) / **25+** (26.x.x) |
| Server software | Paper, Purpur, Pufferfish, Folia, Leaf, and other Paper forks |
| Spigot / CraftBukkit | Loads cleanly; server-brand spoofing is disabled (Paper-only API) |
| Folia | Supported — declared via `folia-supported: true`, fully lock-free, no scheduler use |

---

## Performance

PluginGuard is built to be invisible to your TPS.

- **Zero background threads, zero scheduled tasks** — pure listener-driven.
- **Lock-free hot path** — config is held in an immutable snapshot behind a `@Volatile` reference; readers on every Folia region thread access it without contention.
- **Minimal per-command work** — the command-preprocess listener slices the base command token by index and lowercases only that, so per-event CPU is bounded by the command name length, not the message length.
- **No reflection at runtime** — only at startup, to detect Paper vs. Spigot.

---

## Configuration

The full `config.yml` ships with inline comments explaining every option. A taste:

```yaml
# How to respond when a player tries to enumerate plugins.
hide-mode: "unknown-command"

# Shown when hide-mode is "fake-list".
fake-plugins:
  - "ServerCore"
  - "WorldManager"
  - "CoreProtect"
  - "EveryoneChat"

bypass-permission: "pluginguard.bypass"

protected-commands: [pl, plugins, ver, version, "?", help, about, icanhasbukkit]
block-bukkit-commands: true
redirect-bukkit-commands: false

hide-tab-completion: true
block-unknown-commands: true
block-common-plugin-commands: true

hide-server-brand: true
fake-server-brand: "vanilla"

aggressive-mode: false
```

---

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/pluginguard reload` | Atomically reload the configuration | `pluginguard.reload` (default: op) |
| `/pluginguard status` | Show current protection status | `pluginguard.reload` (default: op) |

Alias: `/pg`

---

## Permissions

| Node | Description | Default |
| --- | --- | --- |
| `pluginguard.bypass` | See the real plugin list and bypass all hiding | op |
| `pluginguard.reload` | Reload PluginGuard configuration | op |

---

## Installation

1. Drop `PluginGuard-<version>.jar` into your `plugins/` folder.
2. Start the server once to generate `plugins/PluginGuard/config.yml`.
3. Edit the config to taste and run `/pluginguard reload`.

That's it. No external dependencies. No database. No web dashboard.

---

## Source & support

- **Source code & issues**: <https://github.com/darkstarworks/PluginGuard>
- **Donate**: <https://ko-fi.com/darkstarworks>
