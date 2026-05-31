[CENTER]
[SIZE=7][B]PluginGuard[/B][/SIZE]
[SIZE=4][COLOR=#888888]Stop players from fingerprinting your server's plugin list.[/COLOR][/SIZE]

[SIZE=3][B]Minecraft 1.21.x  |  26.x.x  |  Paper  |  Purpur  |  Folia  |  Spigot[/B][/SIZE]
[/CENTER]

[HR][/HR]

[SIZE=5][B]Why PluginGuard?[/B][/SIZE]

Knowing which plugins a server runs is the first step in attacking it. A player who knows you run a specific economy plugin, an outdated permissions plugin, or a known-vulnerable utility can target published CVEs or exploit known quirks. Default Bukkit gives that information away to anyone who types [COLOR=#cc6633][I]/pl[/I][/COLOR].

[B]PluginGuard closes every public surface that leaks plugin presence:[/B]
[LIST]
[*][I]/plugins[/I], [I]/pl[/I], [I]/ver[/I], [I]/version[/I], [I]/about[/I], [I]/icanhasbukkit[/I]
[*][I]bukkit:[/I] and [I]minecraft:[/I] prefixed command probing
[*]Tab-completion enumeration via [I]/[tab][/I]
[*]Permission-error vs unknown-command distinction (used to enumerate plugin existence)
[*]Server-list ping / MOTD protocol brand identification
[/LIST]

[HR][/HR]

[SIZE=5][B]Features[/B][/SIZE]

[LIST]
[*][B]Hide Mode[/B] — choose how the server responds to probes:
  [LIST]
  [*][B]unknown-command[/B] — indistinguishable from a typo (most realistic)
  [*][B]empty[/B] — returns [I]Plugins (0):[/I]
  [*][B]fake-list[/B] — return a configurable list of plausible-looking decoys
  [*][B]permission-denied[/B] — pretend the player simply lacks permission
  [/LIST]
[*][B]Plugin Spoofing[/B] — make a hardened production server look like vanilla Paper with two utility plugins.
[*][B]bukkit: / minecraft: Prefix Protection[/B] — block or redirect prefixed-command probes.
[*][B]Tab-Completion Hardening[/B] — strip plugin commands from autocomplete suggestions.
[*][B]Common Plugin Blocklist[/B] — [I]/essentials[/I], [I]/lp[/I], [I]/we[/I], [I]/co[/I], [I]/mv[/I], [I]/dynmap[/I], [I]/gp[/I] and friends all return "Unknown command" — denying the distinction attackers use to enumerate.
[*][B]Server-Brand Spoofing[/B] — return [COLOR=#33aa33][I]vanilla[/I][/COLOR] (or anything you configure) in MOTD / server-list ping responses.
[*][B]Aggressive Mode[/B] — block every plugin command by default; only players with explicit [I]<command>.use[/I] permission may use them.
[*][B]Bypass Permission[/B] — staff with [I]pluginguard.bypass[/I] see the real server, untouched.
[*][B]Hot Reload[/B] — [I]/pluginguard reload[/I] swaps the live configuration atomically.
[/LIST]

[HR][/HR]

[SIZE=5][B]Compatibility[/B][/SIZE]

[LIST]
[*][B]Minecraft:[/B] 1.21.x and 26.x.x
[*][B]Java:[/B] 21+ (1.21.x) / 25+ (26.x.x)
[*][B]Recommended:[/B] Paper, Purpur, Pufferfish, Folia, Leaf and other Paper forks
[*][B]Spigot / CraftBukkit:[/B] loads cleanly — server-brand spoofing is disabled because the underlying event is Paper-only
[*][B]Folia:[/B] fully supported, lock-free, no scheduler use
[/LIST]

[HR][/HR]

[SIZE=5][B]Performance[/B][/SIZE]

PluginGuard is built to be invisible to your TPS.

[LIST]
[*][B]Zero background threads, zero scheduled tasks[/B] — pure listener-driven.
[*][B]Lock-free hot path[/B] — config is held in an immutable snapshot behind a volatile reference, accessed without contention from every Folia region thread.
[*][B]Minimal per-command work[/B] — the command listener slices the base command by index and lowercases only that, so per-event CPU is bounded by the command name length, not the message length.
[*][B]No runtime reflection[/B] — used only at startup to detect Paper vs. Spigot.
[/LIST]

[HR][/HR]

[SIZE=5][B]Configuration[/B][/SIZE]

The full [I]config.yml[/I] ships with inline comments explaining every option. A taste:

[CODE=yaml]
hide-mode: "unknown-command"

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
[/CODE]

[HR][/HR]

[SIZE=5][B]Commands[/B][/SIZE]

[LIST]
[*][B]/pluginguard reload[/B] — atomically reload the configuration
[*][B]/pluginguard status[/B] — show current protection status
[*]Alias: [B]/pg[/B]
[/LIST]

[SIZE=5][B]Permissions[/B][/SIZE]

[LIST]
[*][B]pluginguard.bypass[/B] — see the real plugin list and bypass all hiding (default: op)
[*][B]pluginguard.reload[/B] — reload PluginGuard configuration (default: op)
[/LIST]

[HR][/HR]

[SIZE=5][B]Installation[/B][/SIZE]

[LIST=1]
[*]Drop [B]PluginGuard-<version>.jar[/B] into your [I]plugins/[/I] folder.
[*]Start the server once to generate [I]plugins/PluginGuard/config.yml[/I].
[*]Edit the config to taste and run [I]/pluginguard reload[/I].
[/LIST]

No external dependencies. No database. No web dashboard.

[HR][/HR]

[CENTER]
[SIZE=4][B]Source & support[/B][/SIZE]
[URL=https://github.com/darkstarworks/PluginGuard]GitHub — source & issues[/URL]
[URL=https://ko-fi.com/darkstarworks]Donate on Ko-fi[/URL]
[/CENTER]
