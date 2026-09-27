# Choose what players see

`hide-mode` in `config.yml` decides what `/plugins`, `/version`, `/help` and
`/icanhasbukkit` answer. Change it, then run `/pluginguard reload`.

| `hide-mode` | `/plugins` answers |
| --- | --- |
| `unknown-command` | `Unknown or incomplete command`, like a made-up command. The default, and the hardest to see through |
| `empty` | `Plugins (0):` |
| `fake-list` | The names in `fake-plugins` |
| `permission-denied` | Your server's normal "no permission" message |

## Show a fake plugin list

1. Set `hide-mode: "fake-list"`.
2. Put the names you want to show under `fake-plugins`:

```yaml
hide-mode: "fake-list"
fake-plugins:
  - "ServerCore"
  - "WorldManager"
```

3. Run `/pluginguard reload`.

`/version` then shows `fake-server-brand` and your Minecraft version. To change
the wording itself, see [Messages](../reference/messages.md).

## Show a different server brand

Players see `fake-server-brand` in the server list, on the F3 screen and in the
query answer:

```yaml
hide-server-brand: true
fake-server-brand: "vanilla"
```

{% hint style="info" %}
The F3 screen and the query answer need Paper. On Spigot only the server list
changes.
{% endhint %}

## Let a player use a hidden command

Remove it from `common-plugin-commands` or `protected-commands`, or give the
player `pluginguard.bypass`.
