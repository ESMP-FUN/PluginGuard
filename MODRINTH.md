# PluginGuard

**Stop players from seeing which plugins your server runs.**

Type `/pl` on most servers and you get the full plugin list. That list is a gift to anyone looking for a way in: a plugin with a known bug, an old version, a command to abuse. PluginGuard closes every way players find it out, and warns your staff when someone tries.

Lightweight, no database. Drop it in and you're protected.

---

<details>
<summary><b>What it hides</b></summary>

- `/plugins`, `/pl`, `/ver`, `/version`, `/about`, `/help`, `/icanhasbukkit`
- `/bukkit:...` and `/minecraft:...` commands, like `/bukkit:plugins`
- Commands with a plugin name in front, like `/essentials:home`. The part before the colon *is* the plugin's name
- Plugin commands in the TAB list, and TAB suggestions after a hidden command
- The "You don't have permission" reply, which quietly proves a plugin is installed
- The server software, in the server list **and** on the F3 screen in game
- The list of plugin channels every player's game gets on join, which names plugins like WorldEdit outright
- The query port, which normally hands your full plugin list to anyone who asks

Every blocked command gets exactly the reply a made-up command gets, so players can't tell anything was hidden.

</details>

<details>
<summary><b>Features</b></summary>

- **Choose what players see.** "Unknown command", an empty list, a fake plugin list, or your server's "no permission" message.
- **Fake plugin list.** Make a heavily modded server look like plain Paper with a couple of harmless extras.
- **Blocks popular plugin commands.** `/essentials`, `/lp`, `/we`, `/co`, `/mv`, `/dynmap` and more all answer "Unknown command".
- **Fake server brand.** Show `vanilla`, or anything you like, in the server list and on the F3 screen.
- **Aggressive mode.** Hide *every* plugin command unless a player has permission for it.
- **Warns your staff.** Notices when someone goes looking and tells staff in game. Normal `/help` use is ignored.
- **Discord warnings and automatic kicks.** Get every warning in a Discord channel, and kick or ban probers automatically.
- **Honeypot commands.** Made-up commands no normal player would type. One use and you know who is snooping.
- **Staff bypass.** Staff see the real server and are never counted.
- **Every message editable** in `messages.yml`.
- **Instant reload.** Run `/pluginguard reload`, no restart.

</details>

<details>
<summary><b>Compatibility</b></summary>

| | |
| --- | --- |
| **Minecraft** | 1.21.x, 26.1, 26.2 and 26.3 |
| **Java** | 21 for 1.21.x, 25 for 26.x |
| **Best on** | Paper, Purpur, Pufferfish, Folia, Leaf and other Paper forks |
| **Spigot / CraftBukkit** | Works. The server brand, plugin channels and query answer can't be hidden there |
| **Folia** | Fully supported |

Three downloads: `PluginGuard-<version>.jar` for 1.21.x, `PluginGuard-<version>-mc26.jar` for 26.1 and 26.2, and `PluginGuard-<version>-mc263.jar` for 26.3. Grab the one that matches your server.

</details>

<details>
<summary><b>Installation</b></summary>

1. Put the jar that matches your Minecraft version in your `plugins/` folder.
2. Start the server.

That's it, everything is on by default. To change anything, edit `plugins/PluginGuard/config.yml` and run `/pluginguard reload`. Every setting is explained in the file and in the [guide](https://github.com/ESMP-FUN/PluginGuard/tree/main/docs).

</details>

<details>
<summary><b>Commands & permissions</b></summary>

| Command | What it does |
| --- | --- |
| `/pluginguard reload` | Loads your changes to the config and messages |
| `/pluginguard status` | Shows what is being hidden right now |
| `/pluginguard update` | Checks for a new version and installs it |

Short form: `/pg`. All need `pluginguard.reload`.

| Permission | What it gives | Default |
| --- | --- | --- |
| `pluginguard.bypass` | See the real server, never counted | op |
| `pluginguard.reload` | Use `/pluginguard` | op |
| `pluginguard.alerts` | Get snooping warnings in chat | op |

</details>

---

### Pairs well with Better Anti-Dupe

Running PluginGuard? Take a look at **[Better Anti-Dupe](https://modrinth.com/plugin/better-anti-dupe)** too. It follows every item from where it came from and catches duplication. Hiding your plugin list makes it harder for dupers to know what they're up against, so the two work well together.

---

- **Guide**: <https://github.com/ESMP-FUN/PluginGuard/tree/main/docs>
- **Source & issues**: <https://github.com/ESMP-FUN/PluginGuard>
- **Donate**: <https://ko-fi.com/darkstarworks>
