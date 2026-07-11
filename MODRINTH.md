# PluginGuard

**Stop players from seeing which plugins your server runs.**

Type `/pl` on most servers and you get the full plugin list. That list is a gift to anyone looking for a way in — a known-buggy plugin, an outdated version, a command to abuse. PluginGuard shuts every door players use to find that out, and lets you show them whatever you like instead.

Lightweight, no dependencies, no database. Drop it in and you're protected.

---

<details>
<summary><b>What it protects against</b></summary>

Players have a lot of little tricks for figuring out your setup. PluginGuard blocks all of them:

- `/plugins`, `/pl`, `/ver`, `/version`, `/about`, `/icanhasbukkit`
- `bukkit:` and `minecraft:` prefixed commands (e.g. `/bukkit:plugins`)
- Namespaced commands like `/essentials:home` — the part before the colon *is* the plugin name
- Tab-completion probing (pressing TAB to reveal commands)
- The "unknown command" vs "you don't have permission" difference, which quietly confirms a plugin exists
- The server brand in the server-list ping **and** in the in-game F3 debug screen

</details>

<details>
<summary><b>Features</b></summary>

- **Choose what players see.** Reply to probes with a fake "unknown command", an empty list, a list of convincing decoy plugins, or a "no permission" message — your call.
- **Fake plugin list.** Make a heavily-modded server look like plain vanilla Paper with a couple of harmless utilities.
- **Command blocking.** Common plugin commands (`/essentials`, `/lp`, `/we`, `/co`, `/mv`, `/dynmap`, and more) all just return "Unknown command".
- **Tab-completion cleanup.** Plugin commands never show up in autocomplete.
- **Server-brand spoofing.** Show `vanilla` (or anything you want) both in the server list and in the F3 screen. Most similar plugins only cover the server list — PluginGuard covers both.
- **Aggressive mode.** Optionally hide *every* plugin command unless a player has explicit permission for it.
- **Probe detection & alerts.** PluginGuard notices when someone is poking around and alerts your staff in-game. Casual `/help` use is ignored; a real probing pattern trips it fast.
- **Honeypot commands.** List fake commands no normal player would ever type. One hit and you know exactly who's snooping.
- **Staff bypass.** Anyone with the bypass permission sees the real server and never trips an alert.
- **Instant reload.** Change the config and run `/pluginguard reload` — no restart.

</details>

<details>
<summary><b>Compatibility</b></summary>

| | |
| --- | --- |
| **Minecraft** | 1.21.x and 26.x |
| **Java** | 21+ (for 1.21.x) / 25+ (for 26.x) |
| **Best on** | Paper, Purpur, Pufferfish, Folia, Leaf, and other Paper forks |
| **Spigot / CraftBukkit** | Works fine — only the server-brand spoofing turns off (it needs Paper) |
| **Folia** | Fully supported |

Two downloads are provided: `PluginGuard-<version>.jar` for Minecraft 1.21.x, and `PluginGuard-<version>-mc26.jar` for 26.x. Grab the one that matches your server.

</details>

<details>
<summary><b>Installation</b></summary>

1. Download the jar that matches your Minecraft version and drop it in your `plugins/` folder.
2. Start the server once — this creates `plugins/PluginGuard/config.yml`.
3. Edit the config however you like, then run `/pluginguard reload`.

That's it. No external dependencies, no database, no web dashboard.

</details>

<details>
<summary><b>Configuration example</b></summary>

Every option is explained with inline comments in the generated `config.yml`. Here's the gist:

```yaml
# How to respond when someone tries to list your plugins.
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
block-namespaced-commands: true
redirect-bukkit-commands: false

hide-tab-completion: true
block-unknown-commands: true
block-common-plugin-commands: true

hide-server-brand: true
fake-server-brand: "vanilla"

aggressive-mode: false

# Probe logging & detection
logging:
  log-to-file: false
  log-individual-probes: false
  detection:
    enabled: true
    score-threshold: 5
    window-seconds: 60
    alert-cooldown-seconds: 300
    notify-permission: "pluginguard.alerts"

# Fake commands no real player would type. A single hit fires an alert.
honeypot-commands:
  - "staffchat"
  - "adminchat"
  - "modchat"
  - "opme"
```

</details>

<details>
<summary><b>Commands & permissions</b></summary>

| Command | What it does | Permission |
| --- | --- | --- |
| `/pluginguard reload` | Reload the config on the fly | `pluginguard.reload` (op) |
| `/pluginguard status` | Show current protection status | `pluginguard.reload` (op) |

Alias: `/pg`

| Permission | What it grants | Default |
| --- | --- | --- |
| `pluginguard.bypass` | See the real plugins and server brand; never tracked | op |
| `pluginguard.reload` | Reload the configuration | op |
| `pluginguard.alerts` | Get in-game alerts when someone is probing | op |

</details>

---

### Pairs perfectly with AntiDupePro

Running PluginGuard? Take a look at **[AntiDupePro](https://modrinth.com/plugin/antidupepro)** too — a chain-of-custody item-flow auditor that catches item duplication. It benefits directly from PluginGuard hiding `/plugin` and friends, so the two make a great team.

---

- **Source & issues**: <https://github.com/ESMP-FUN/PluginGuard>
- **Donate**: <https://ko-fi.com/darkstarworks>
