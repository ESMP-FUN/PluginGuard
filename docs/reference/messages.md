# Messages (messages.yml)

Change any text PluginGuard shows in `plugins/PluginGuard/messages.yml`, then
run `/pluginguard reload`.

* Colours use `&` codes: `&a` green, `&c` red, `&e` yellow, `&6` gold, `&7` gray, `&f` white
* Words in `{curly brackets}` are filled in for you. Keep them as they are
* A key missing from your file uses the built-in text

{% hint style="info" %}
Two replies are not in the file on purpose: "Unknown or incomplete command" and
the "no permission" message. PluginGuard copies them from your server so they
match exactly. Change the server's own text and PluginGuard follows.
{% endhint %}

| Key | Shown | Fills in |
| --- | --- | --- |
| `fake-plugin-list` | `/plugins` with `fake-list` | `{count}`, `{plugins}` |
| `empty-plugin-list` | `/plugins` with `empty` | |
| `fake-version` | `/version` with `fake-list` | `{brand}`, `{version}` |
| `version-disabled` | `/version` with `empty` | |
| `fake-help` | `/help` with `empty` or `fake-list` | |
| `fake-icanhasbukkit` | `/icanhasbukkit` with `empty` or `fake-list` | |
| `alert` | Staff warning | `{player}`, `{score}`, `{seconds}`, `{tried}` |
| `no-permission` | `/pluginguard` without permission | |
| `reloaded` | After `/pluginguard reload` | |
| `unknown-subcommand` | `/pluginguard` with a wrong option | |
| `help` | `/pluginguard` on its own | |
| `status` | `/pluginguard status` | see the file |
| `status-on`, `status-off`, `status-real-brand` | Words used in `status` | |
