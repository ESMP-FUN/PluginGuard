# Commands & permissions

## Commands

All need `pluginguard.reload` (operators by default). `/pg` works as a short form.

| Command | What it does |
| --- | --- |
| `/pluginguard` | Lists the commands |
| `/pluginguard reload` | Loads your changes to `config.yml` and `messages.yml` |
| `/pluginguard status` | Shows what is being hidden right now |
| `/pluginguard update` | Checks for a new version |
| `/pluginguard update download` | Downloads it. It installs on the next restart |
| `/pluginguard update restore` | Goes back to the version before the last update |
| `/pluginguard update ignore <version>` | Stops reminding you about that version |
| `/pluginguard update unignore <version>` | Undoes the above |
| `/pluginguard update status` | Shows the last update check |

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `pluginguard.bypass` | op | Sees the real server, never counted as snooping |
| `pluginguard.reload` | op | Uses `/pluginguard` |
| `pluginguard.alerts` | op | Gets snooping warnings in chat |
