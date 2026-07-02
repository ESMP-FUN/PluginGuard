[CENTER]
[SIZE=7][B]PluginGuard[/B][/SIZE]
[SIZE=4][COLOR=#888888]Stop players from seeing which plugins your server runs.[/COLOR][/SIZE]

[SIZE=3][B]Minecraft 1.21.x  |  26.x  |  Paper  |  Purpur  |  Folia  |  Spigot[/B][/SIZE]
[/CENTER]



Type [COLOR=#cc6633][I]/pl[/I][/COLOR] on most servers and you get the full plugin list. That list is a gift to anyone looking for a way in — a known-buggy plugin, an outdated version, a command to abuse. PluginGuard shuts every door players use to find that out, and lets you show them whatever you like instead.

Lightweight, no dependencies, no database. Drop it in and you're protected.



[SPOILER=What it protects against]
Players have a lot of little tricks for figuring out your setup. PluginGuard blocks all of them:
[LIST]
[*][I]/plugins[/I], [I]/pl[/I], [I]/ver[/I], [I]/version[/I], [I]/about[/I], [I]/icanhasbukkit[/I]
[*][I]bukkit:[/I] and [I]minecraft:[/I] prefixed commands (e.g. [I]/bukkit:plugins[/I])
[*]Namespaced commands like [I]/essentials:home[/I] — the part before the colon [B]is[/B] the plugin name
[*]Tab-completion probing (pressing TAB to reveal commands)
[*]The "unknown command" vs "you don't have permission" difference, which quietly confirms a plugin exists
[*]The server brand in the server-list ping [B]and[/B] in the in-game F3 debug screen
[/LIST]
[/SPOILER]

[SPOILER=Features]
[LIST]
[*][B]Choose what players see.[/B] Reply to probes with a fake "unknown command", an empty list, a list of convincing decoy plugins, or a "no permission" message — your call.
[*][B]Fake plugin list.[/B] Make a heavily-modded server look like plain vanilla Paper with a couple of harmless utilities.
[*][B]Command blocking.[/B] Common plugin commands ([I]/essentials[/I], [I]/lp[/I], [I]/we[/I], [I]/co[/I], [I]/mv[/I], [I]/dynmap[/I], and more) all just return "Unknown command".
[*][B]Tab-completion cleanup.[/B] Plugin commands never show up in autocomplete.
[*][B]Server-brand spoofing.[/B] Show [COLOR=#33aa33][I]vanilla[/I][/COLOR] (or anything you want) both in the server list and in the F3 screen. Most similar plugins only cover the server list — PluginGuard covers both.
[*][B]Aggressive mode.[/B] Optionally hide [I]every[/I] plugin command unless a player has explicit permission for it.
[*][B]Probe detection & alerts.[/B] PluginGuard notices when someone is poking around and alerts your staff in-game. Casual [I]/help[/I] use is ignored; a real probing pattern trips it fast.
[*][B]Honeypot commands.[/B] List fake commands no normal player would ever type. One hit and you know exactly who's snooping.
[*][B]Staff bypass.[/B] Anyone with the bypass permission sees the real server and never trips an alert.
[*][B]Instant reload.[/B] Change the config and run [I]/pluginguard reload[/I] — no restart.
[/LIST]
[/SPOILER]

[SPOILER=Compatibility]
[LIST]
[*][B]Minecraft:[/B] 1.21.x and 26.x
[*][B]Java:[/B] 21+ (for 1.21.x) / 25+ (for 26.x)
[*][B]Best on:[/B] Paper, Purpur, Pufferfish, Folia, Leaf, and other Paper forks
[*][B]Spigot / CraftBukkit:[/B] works fine — only the server-brand spoofing turns off (it needs Paper)
[*][B]Folia:[/B] fully supported
[/LIST]
Two downloads are provided: [B]PluginGuard-<version>.jar[/B] for Minecraft 1.21.x, and [B]PluginGuard-<version>-mc26.jar[/B] for 26.x. Grab the one that matches your server.
[/SPOILER]

[SPOILER=Installation]
[LIST=1]
[*]Download the jar that matches your Minecraft version and drop it in your [I]plugins/[/I] folder.
[*]Start the server once — this creates [I]plugins/PluginGuard/config.yml[/I].
[*]Edit the config however you like, then run [I]/pluginguard reload[/I].
[/LIST]
That's it. No external dependencies, no database, no web dashboard.
[/SPOILER]

[SPOILER=Configuration example]
Every option is explained with inline comments in the generated [I]config.yml[/I]. Here's the gist:
[CODE=yaml]
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
[/CODE]
[/SPOILER]

[SPOILER=Commands & permissions]
[LIST]
[*][B]/pluginguard reload[/B] — reload the config on the fly ([I]pluginguard.reload[/I], op)
[*][B]/pluginguard status[/B] — show current protection status ([I]pluginguard.reload[/I], op)
[*]Alias: [B]/pg[/B]
[/LIST]
[LIST]
[*][B]pluginguard.bypass[/B] — see the real plugins and server brand; never tracked (default: op)
[*][B]pluginguard.reload[/B] — reload the configuration (default: op)
[*][B]pluginguard.alerts[/B] — get in-game alerts when someone is probing (default: op)
[/LIST]
[/SPOILER]



[SIZE=5][B]Pairs perfectly with AntiDupePro[/B][/SIZE]

Running PluginGuard? Take a look at [URL=https://www.spigotmc.org/resources/antidupepro.135719/][B]AntiDupePro[/B][/URL] too — a chain-of-custody item-flow auditor that catches item duplication. It benefits directly from PluginGuard hiding [I]/plugin[/I] and friends, so the two make a great team.



[CENTER]
[SIZE=4][B]Source & support[/B][/SIZE]
[URL=https://github.com/darkstarworks/PluginGuard]GitHub — source & issues[/URL]
[URL=https://ko-fi.com/darkstarworks]Donate on Ko-fi[/URL]
[/CENTER]
