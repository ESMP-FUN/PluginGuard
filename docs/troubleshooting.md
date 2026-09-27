# Troubleshooting

<details>

<summary>A player can't use a command they should have</summary>

It is probably in `common-plugin-commands`, or `aggressive-mode` is on.

* Remove the command from `common-plugin-commands`, or
* With `aggressive-mode`, give the player `<command>.use`, like `home.use`

Then run `/pluginguard reload`.

</details>

<details>

<summary>A client mod stopped working (WorldEdit CUI and similar)</summary>

`hide-plugin-channels` hides the channel the mod needs. Add it to
`allowed-plugin-channels`, then have the player rejoin:

```yaml
allowed-plugin-channels:
  - "worldedit:cui"
```

</details>

<details>

<summary>I still see the real plugin list</summary>

You have `pluginguard.bypass`, which operators get by default. Test with an
account that is not an operator.

</details>

<details>

<summary>The F3 screen still shows Paper</summary>

* Check `hide-server-brand: true`
* Look in the console at startup for "Can't hide the in-game server brand". Your
  server version may not support it yet. Please report it with the version.
* On Spigot this can't be hidden

</details>

<details>

<summary>Staff get too many warnings</summary>

Raise `score-threshold` or `alert-cooldown-seconds`, or remove commands your
players use legitimately from `common-plugin-commands`.

</details>

<details>

<summary>Discord warnings don't arrive</summary>

The console says why. "must start with https://" means the URL was pasted
wrong. "Discord refused the warning (HTTP 404)" means the webhook was deleted in Discord; make a new
one.

</details>
