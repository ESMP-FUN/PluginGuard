# Catch players who go looking

PluginGuard counts it when a player tries a hidden command and warns your staff
when someone clearly goes looking. It is on by default.

## How it decides

Each attempt adds points. Once a player reaches `score-threshold` points within
`window-seconds`, staff are warned.

| Points | For |
| --- | --- |
| 5 | A honeypot command |
| 3 | `/bukkit:...`, `/minecraft:...`, `/essentials:home` style commands, `/icanhasbukkit` |
| 2 | `/pl`, `/plugins`, `/ver`, `/version`, `/about` |
| 1 | One of the `common-plugin-commands` |

With the defaults (5 points within 60 seconds), `/pl` followed by `/ver` is
enough. `/help` and `/?` never count. Players with `pluginguard.bypass` are
never counted.

## Who gets warned

* Online players with `pluginguard.alerts` (operators by default)
* The console, always
* A Discord channel, if you set one up below

The same player can't cause another warning for `alert-cooldown-seconds`
(5 minutes by default).

## Set up honeypots

A honeypot is a made-up command only someone searching your server would try.
One use is a warning straight away.

```yaml
honeypot-commands:
  - "staffchat"
  - "opme"
```

Pick names that don't exist on your server.

## Get warnings in Discord

1. In Discord, open the channel's settings, then **Integrations**, then **Webhooks**.
2. Create a webhook and copy its URL.
3. Paste it into `config.yml`:

```yaml
logging:
  detection:
    discord-webhook: "https://discord.com/api/webhooks/..."
```

4. Run `/pluginguard reload`.

## Kick or ban automatically

`alert-commands` runs console commands when a player sets off a warning.
`%player%` is their name, `%uuid%` their UUID, `%score%` their points.

```yaml
logging:
  detection:
    alert-commands:
      - "kick %player% Please don't probe the server."
```

## Keep a record

Set `log-to-file: true` to write every attempt to
`plugins/PluginGuard/probes.log`. Past `log-max-size-mb` (5 MB) it moves to
`probes.old.log` and a new file starts.
