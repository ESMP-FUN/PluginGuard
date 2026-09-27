# PluginGuard

Hide which plugins your Minecraft server runs, and find out who goes looking.

PluginGuard blocks `/plugins`, `/version`, namespaced commands like
`/essentials:home`, TAB probing, the server brand, plugin channels and the query
answer. Every blocked command gets the same reply a made-up command gets, so
players can't tell what was hidden. Staff are warned in chat or Discord when
someone probes.

**[Read the guide](docs/README.md)** to install and set it up.

## Downloads

| Your server | Download | Java |
| --- | --- | --- |
| Minecraft 1.21.x | `PluginGuard-<version>.jar` | 21 |
| Minecraft 26.1 or 26.2 | `PluginGuard-<version>-mc26.jar` | 25 |
| Minecraft 26.3 | `PluginGuard-<version>-mc263.jar` | 25 |

Works on Paper, Purpur, Folia and other Paper forks. Spigot works too, without
the brand, channel and query hiding.

Get it from [Modrinth](https://modrinth.com/plugin/pluginguard) or
[GitHub Releases](https://github.com/ESMP-FUN/PluginGuard/releases).

## Building

```
./gradlew build -Pmc=21     # 1.21.x, needs JDK 21
./gradlew build -Pmc=26     # 26.1 and 26.2, needs JDK 25
./gradlew build -Pmc=263    # 26.3, needs JDK 25
```

The jar lands in `build/libs/`.

## Support

[Ko-fi](https://ko-fi.com/darkstarworks)
