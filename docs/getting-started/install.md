# Install PluginGuard

PluginGuard works as soon as it is installed. Everything it hides is on by
default.

## 1. Pick the right download

| Your server | Download |
| --- | --- |
| Minecraft 1.21.x | `PluginGuard-<version>.jar` |
| Minecraft 26.1 or 26.2 | `PluginGuard-<version>-mc26.jar` |
| Minecraft 26.3 | `PluginGuard-<version>-mc263.jar` |

## 2. Install it

1. Stop the server.
2. Put the jar in your `plugins/` folder.
3. Start the server.

That is the whole job.

## 3. Give your staff the bypass

Players with `pluginguard.bypass` see the real server and are never counted as
snooping. Operators have it already. With LuckPerms:

```
/lp group staff permission set pluginguard.bypass true
```

## 4. Check it

Join as a normal player (not an operator) and type `/pl`. You should get
`Unknown or incomplete command`, the same as for a made-up command.

Run `/pluginguard status` as an operator to see everything that is on.

{% hint style="info" %}
Updates are checked for you. Staff are told when one is out, and
`/pluginguard update download` installs it on the next restart.
{% endhint %}
