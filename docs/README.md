# PluginGuard

Hide which plugins your server runs.

A plugin list tells anyone looking for trouble exactly where to start: a plugin
with a known bug, an old version, a command to abuse. PluginGuard closes the ways
players find that list, and warns your staff when someone tries.

## What it hides

* `/plugins`, `/pl`, `/version`, `/ver`, `/about`, `/help`, `/icanhasbukkit`
* `/bukkit:...` and `/minecraft:...` commands
* Commands with a plugin name in front, like `/essentials:home`
* Plugin commands in the TAB list, and TAB suggestions after a hidden command
* The "You don't have permission" reply that proves a plugin is installed
* The server software in the server list, on the F3 screen and in the query answer
* The list of plugin channels every player's game gets when they join

Every blocked command gets the same reply a made-up command gets, so a player
can't tell what was hidden.

## Will it work on my server?

| | |
| --- | --- |
| Server software | **Paper**, **Purpur**, **Folia** or another Paper fork. **Spigot** works too, see below |
| Minecraft version | **1.21.x**, **26.1**, **26.2** or **26.3** |
| Java | **21** for 1.21.x, **25** for 26.x |

{% hint style="info" %}
On Spigot, commands, TAB and warnings all work. The server brand, plugin
channels and query answer can't be hidden there, because that needs Paper.
{% endhint %}

## Where to start

* **New here:** [Install PluginGuard](getting-started/install.md). Two minutes.
* **Want a fake plugin list instead:** [Choose what players see](getting-started/what-players-see.md).
* **Want to know who is snooping:** [Catch players who go looking](catching-probers.md).
