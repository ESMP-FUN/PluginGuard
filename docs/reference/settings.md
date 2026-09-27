# Settings (config.yml)

Every setting in `plugins/PluginGuard/config.yml`. Run `/pluginguard reload`
after a change.

<details>

<summary>What players see</summary>

| Setting | Default | What it does |
| --- | --- | --- |
| `hide-mode` | `unknown-command` | What hidden commands answer: `unknown-command`, `empty`, `fake-list` or `permission-denied`. See [Choose what players see](../getting-started/what-players-see.md) |
| `fake-plugins` | 4 names | The names `/plugins` shows with `fake-list` |
| `bypass-permission` | `pluginguard.bypass` | Players with this see the real server |

</details>

<details>

<summary>Commands</summary>

| Setting | Default | What it does |
| --- | --- | --- |
| `protected-commands` | `pl`, `plugins`, `ver`, `version`, `?`, `help`, `about`, `icanhasbukkit` | Answer with `hide-mode` |
| `block-bukkit-commands` | `true` | Blocks every `/bukkit:...` and `/minecraft:...` command |
| `block-namespaced-commands` | `true` | Blocks commands with a plugin name in front, like `/essentials:home` |
| `redirect-bukkit-commands` | `false` | `/bukkit:plugins` answers with `hide-mode` instead of "Unknown command" |
| `hide-tab-completion` | `true` | Takes hidden commands out of TAB, and blocks suggestions after them |
| `block-unknown-commands` | `true` | A plugin command the player isn't allowed to use answers "Unknown command" instead of "no permission" |
| `common-plugin-commands` | 19 commands | Popular plugin commands players try, like `/lp` and `/we` |
| `block-common-plugin-commands` | `true` | Blocks the list above |
| `aggressive-mode` | `false` | Hides every plugin command unless the player has `<command>.use`, like `home.use` |

</details>

<details>

<summary>Server software</summary>

| Setting | Default | What it does |
| --- | --- | --- |
| `hide-server-brand` | `true` | Shows `fake-server-brand` in the server list, on F3 and in the query answer |
| `fake-server-brand` | `vanilla` | The brand to show |
| `hide-plugin-channels` | `true` | Empties the list of plugin channels players' games get on join. Paper only |
| `allowed-plugin-channels` | empty | Channels to keep, like `worldedit:cui` for the WorldEdit CUI mod |

The plugin list in the query answer (`enable-query` in `server.properties`) is
always emptied.

</details>

<details>

<summary>Catching players who go looking (logging:)</summary>

| Setting | Default | What it does |
| --- | --- | --- |
| `log-to-file` | `false` | Writes every attempt to `probes.log` |
| `log-max-size-mb` | `5` | Size at which `probes.log` moves to `probes.old.log`. `0` never |
| `log-individual-probes` | `false` | Writes every attempt to the console |
| `detection.enabled` | `true` | Turns warnings on or off |
| `detection.score-threshold` | `5` | Points that cause a warning |
| `detection.window-seconds` | `60` | How long points count |
| `detection.alert-cooldown-seconds` | `300` | Wait before the same player can cause another warning |
| `detection.notify-permission` | `pluginguard.alerts` | Who gets warnings in chat |
| `detection.alert-commands` | empty | Console commands to run on a warning. `%player%`, `%uuid%`, `%score%` |
| `detection.discord-webhook` | empty | Discord webhook URL for warnings |
| `honeypot-commands` | `staffchat`, `adminchat`, `modchat`, `opme` | Made-up commands that cause a warning on first use |

</details>

<details>

<summary>Updates (update:)</summary>

| Setting | Default | What it does |
| --- | --- | --- |
| `mode` | `notify` | `notify`, `check-only`, `download`, `auto-stage` or `off` |
| `check-interval-hours` | `6` | How often to check |

</details>
