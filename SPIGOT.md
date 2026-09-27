[CENTER]
[SIZE=7][B]PluginGuard[/B][/SIZE]
[SIZE=4][COLOR=#888888]Stop players from seeing which plugins your server runs.[/COLOR][/SIZE]

[SIZE=3][B]Minecraft 1.21.x  |  26.1 - 26.3  |  Paper  |  Purpur  |  Folia  |  Spigot[/B][/SIZE]
[/CENTER]



Type [COLOR=#cc6633][I]/pl[/I][/COLOR] on most servers and you get the full plugin list. That list is a gift to anyone looking for a way in: a plugin with a known bug, an old version, a command to abuse. PluginGuard closes every way players find it out, and warns your staff when someone tries.

Lightweight, no database. Drop it in and you're protected.



[SPOILER=What it hides]
[LIST]
[*][I]/plugins[/I], [I]/pl[/I], [I]/ver[/I], [I]/version[/I], [I]/about[/I], [I]/help[/I], [I]/icanhasbukkit[/I]
[*][I]/bukkit:...[/I] and [I]/minecraft:...[/I] commands, like [I]/bukkit:plugins[/I]
[*]Commands with a plugin name in front, like [I]/essentials:home[/I]. The part before the colon [B]is[/B] the plugin's name
[*]Plugin commands in the TAB list, and TAB suggestions after a hidden command
[*]The "You don't have permission" reply, which quietly proves a plugin is installed
[*]The server software, in the server list [B]and[/B] on the F3 screen in game
[*]The list of plugin channels every player's game gets on join, which names plugins like WorldEdit outright
[*]The query port, which normally hands your full plugin list to anyone who asks
[/LIST]
Every blocked command gets exactly the reply a made-up command gets, so players can't tell anything was hidden.
[/SPOILER]

[SPOILER=Features]
[LIST]
[*][B]Choose what players see.[/B] "Unknown command", an empty list, a fake plugin list, or your server's "no permission" message.
[*][B]Fake plugin list.[/B] Make a heavily modded server look like plain Paper with a couple of harmless extras.
[*][B]Blocks popular plugin commands.[/B] [I]/essentials[/I], [I]/lp[/I], [I]/we[/I], [I]/co[/I], [I]/mv[/I], [I]/dynmap[/I] and more all answer "Unknown command".
[*][B]Fake server brand.[/B] Show [COLOR=#33aa33][I]vanilla[/I][/COLOR], or anything you like, in the server list and on the F3 screen.
[*][B]Aggressive mode.[/B] Hide [I]every[/I] plugin command unless a player has permission for it.
[*][B]Warns your staff.[/B] Notices when someone goes looking and tells staff in game. Normal [I]/help[/I] use is ignored.
[*][B]Discord warnings and automatic kicks.[/B] Get every warning in a Discord channel, and kick or ban probers automatically.
[*][B]Honeypot commands.[/B] Made-up commands no normal player would type. One use and you know who is snooping.
[*][B]Staff bypass.[/B] Staff see the real server and are never counted.
[*][B]Every message editable[/B] in [I]messages.yml[/I].
[*][B]Instant reload.[/B] Run [I]/pluginguard reload[/I], no restart.
[/LIST]
[/SPOILER]

[SPOILER=Compatibility]
[LIST]
[*][B]Minecraft:[/B] 1.21.x, 26.1, 26.2 and 26.3
[*][B]Java:[/B] 21 for 1.21.x, 25 for 26.x
[*][B]Best on:[/B] Paper, Purpur, Pufferfish, Folia, Leaf and other Paper forks
[*][B]Spigot / CraftBukkit:[/B] works. The server brand, plugin channels and query answer can't be hidden there
[*][B]Folia:[/B] fully supported
[/LIST]
Three downloads: [B]PluginGuard-<version>.jar[/B] for 1.21.x, [B]PluginGuard-<version>-mc26.jar[/B] for 26.1 and 26.2, and [B]PluginGuard-<version>-mc263.jar[/B] for 26.3. Grab the one that matches your server.
[/SPOILER]

[SPOILER=Installation]
[LIST=1]
[*]Put the jar that matches your Minecraft version in your [I]plugins/[/I] folder.
[*]Start the server.
[/LIST]
That's it, everything is on by default. To change anything, edit [I]plugins/PluginGuard/config.yml[/I] and run [I]/pluginguard reload[/I]. Every setting is explained in the file and in the [URL=https://github.com/ESMP-FUN/PluginGuard/tree/main/docs]guide[/URL].
[/SPOILER]

[SPOILER=Commands & permissions]
[LIST]
[*][B]/pluginguard reload[/B]: loads your changes to the config and messages
[*][B]/pluginguard status[/B]: shows what is being hidden right now
[*][B]/pluginguard update[/B]: checks for a new version and installs it
[*]Short form [B]/pg[/B]. All need [I]pluginguard.reload[/I]
[/LIST]
[LIST]
[*][B]pluginguard.bypass[/B]: see the real server, never counted (default: op)
[*][B]pluginguard.reload[/B]: use /pluginguard (default: op)
[*][B]pluginguard.alerts[/B]: get snooping warnings in chat (default: op)
[/LIST]
[/SPOILER]



[SIZE=5][B]Pairs well with AntiDupePro[/B][/SIZE]

Running PluginGuard? Take a look at [URL=https://www.spigotmc.org/resources/antidupepro.135719/][B]AntiDupePro[/B][/URL] too. It follows every item from where it came from and catches duplication. Hiding your plugin list makes it harder for dupers to know what they're up against, so the two work well together.



[CENTER]
[SIZE=4][B]Source & support[/B][/SIZE]
[URL=https://github.com/ESMP-FUN/PluginGuard/tree/main/docs]Guide[/URL]
[URL=https://github.com/ESMP-FUN/PluginGuard]GitHub: source & issues[/URL]
[URL=https://ko-fi.com/darkstarworks]Donate on Ko-fi[/URL]
[/CENTER]
