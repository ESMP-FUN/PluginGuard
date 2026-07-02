# PluginGuard

#### Protect your server by hiding installed plugins from users

Supports Minecraft **1.21.x** (Java 21+) and **26.x** (Java 25+) — one codebase, two jars:
`PluginGuard-<version>.jar` for 1.21.x servers, `PluginGuard-<version>-mc26.jar` for 26.x servers
(build with `./gradlew shadowJar -Pmc=21` or `-Pmc=26`).
Works on Paper, Purpur, Pufferfish, Folia, and other Paper forks. 
Spigot/Bukkit will load the plugin but server-brand spoofing is disabled (Paper-only API).

> Donate: https://ko-fi.com/darkstarworks

### Features

- **Hide Mode** — choose from several types of "Access Denied" responses
- **Plugin Spoofing** — return a configurable list of fake plugins
- **Optional Bypass Permission**
- **High-level Bypass Protection**
- **Command Redirection**
- **Advanced Protection**
- **Custom Protection**
- **Server Metadata Protection**
- **Optional Server Brand Spoofing** — return e.g. "Vanilla" instead of "Paper", in both the server-list ping *and* the in-game F3 / `minecraft:brand` channel
- **Optional Aggressive Mode** — block everything; see config below
- **Probe Logging & Detection** — record probe attempts and alert admins when a player crosses a weighted-score threshold
- **Honeypot Commands** — admin-defined fake commands; a single hit fires an immediate alert
- **Folia-compatible**

## Configuration (`config.yml`)

### Basic settings

Hide Mode options:
- `"unknown-command"` — shows "Unknown command" (most realistic)
- `"empty"` — shows `Plugins (0):`
- `"fake-list"` — shows the configured fake plugins below
- `"permission-denied"` — shows a permission error

```yaml
hide-mode: "unknown-command"
```

Fake plugins to display when `hide-mode: "fake-list"`
(use vanilla-sounding names to appear legitimate):

```yaml
fake-plugins:
  - "ServerCore"
  - "WorldManager"
  - "CoreProtect"
  - "EveryoneChat"
```

Permission node to bypass all plugin hiding (staff/admin only):

```yaml
bypass-permission: "pluginguard.bypass"
```

### Command protection

Commands to intercept and hide (supports `bukkit:` and `minecraft:` prefixes):

```yaml
protected-commands:
  - "pl"
  - "plugins"
  - "ver"
  - "version"
  - "?"
  - "help"
  - "about"
  - "icanhasbukkit"
```

Block all `bukkit:` and `minecraft:` prefixed commands (prevents probing):

```yaml
block-bukkit-commands: true
```

Redirect `bukkit:` commands to the spoofed list instead of blocking
(only works if `block-bukkit-commands: true`):

```yaml
redirect-bukkit-commands: false
```

Block **all** namespaced commands (`/essentials:home`, `/luckperms:lp`, ...) — the
namespace before the colon is the plugin's name, so one namespaced command confirms
a plugin exists even when its bare alias is hidden. Also strips namespaced entries
from tab completion:

```yaml
block-namespaced-commands: true
```

### Tab-completion protection

Remove plugin commands from tab-completion to prevent `/[tab]` probing:

```yaml
hide-tab-completion: true
```

### Advanced protection

Return "Unknown command" even when the player simply lacks permission —
prevents probing for plugin existence via permission responses:

```yaml
block-unknown-commands: true
```

Common plugin commands to block (case-insensitive):

```yaml
common-plugin-commands:
  - "essentials"
  - "ess"
  - "worldedit"
  - "we"
  - "luckperms"
  - "lp"
  - "coreprotect"
  - "co"
  - "vault"
  - "multiverse"
  - "mv"
  - "citizens"
  - "npc"
  - "clearlag"
  - "dynmap"
  - "griefprevention"
  - "gp"
  - "holographicdisplays"
  - "hd"
```

```yaml
block-common-plugin-commands: true
```

### Server-metadata protection

Hide server software in the server-list ping/MOTD **and** the in-game
`minecraft:brand` channel that the F3 screen and client mods read (Paper/Folia only;
on Spigot this degrades to ping-only):

```yaml
hide-server-brand: true
fake-server-brand: "vanilla"
```

### Probe logging & detection

PluginGuard can record probe attempts and alert online admins when a player shows
a pattern of plugin-enumeration behavior. All I/O is dispatched asynchronously so
the command hot path stays cheap, and per-player state is cleared on quit.

```yaml
logging:
  # Append every probe attempt to plugins/PluginGuard/probes.log
  log-to-file: false
  # Also log every individual probe to the server console (noisy)
  log-individual-probes: false

  detection:
    enabled: true
    score-threshold: 5
    window-seconds: 60
    alert-cooldown-seconds: 300
    notify-permission: "pluginguard.alerts"
```

Weights per probe category:

| Category | Weight | Examples |
| --- | --- | --- |
| Honeypot | 5 | Anything listed under `honeypot-commands` (single hit triggers) |
| High | 3 | `bukkit:foo`, `minecraft:foo`, `/icanhasbukkit` |
| Medium | 2 | `/pl`, `/plugins`, `/ver`, `/version`, `/about` |
| Low | 1 | `/lp`, `/we`, `/co`, `/mv`, `/dynmap`, ... (common plugins) |

`/help` and `/?` are deliberately never tracked — too commonly legitimate.

### Honeypot commands

Fake commands no legitimate user would type. A single attempt is enough to fire
the detector at the default threshold, so honeypots act as a near-zero-false-positive
tripwire. Invent plausible-sounding names an attacker might try:

```yaml
honeypot-commands:
  - "staffchat"
  - "adminchat"
  - "modchat"
  - "opme"
```

### Aggressive mode

Blocks **all** plugin commands for players without `<command>.use` permission.
This will hide even beneficial plugin commands for regular players — use only if
you want maximum security and are willing to manually grant per-command permissions.

```yaml
aggressive-mode: false
```

## Commands

- `/pluginguard reload` — reload config (requires `pluginguard.reload`)
- `/pluginguard status` — show current protection status

## Permissions

- `pluginguard.bypass` — see the real plugin list and bypass all hiding (default: op)
- `pluginguard.reload` — reload PluginGuard configuration (default: op)
- `pluginguard.alerts` — receive in-game probe-detector alerts (default: op)
