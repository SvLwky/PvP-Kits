# PvPKits

**PvPKits** is a lightweight utility mod built using the **Fabric Loader**. It provides server operators with a seamless way to save, manage, and deploy per-player inventory snapshots using robust command-based NBT persistence.

---

## Features

* **Per-Player Kit Management**: Save and load exact inventory states customized for PvP scenarios.
* **NBT File Storage**: Kits are stored securely as individual NBT files directly within the Fabric config directory (`config/pvpkits/`).
* **Operator Security**: Restricted to operators (`Permissions.COMMANDS_MODERATOR`) to prevent abuse in survival and hardcore environments.
* **Intelligent Tab-Completion**: Built-in suggestion providers automatically populate active kit names when typing out load or delete commands.

---

## Commands & Usage

All commands use the base prefix `/pk` and require operator-level permissions.

| Command | Description |
| :--- | :--- |
| `/pk save <name>` | Saves your current inventory state under the specified kit name. |
| `/pk load <name>` | Replaces your current inventory with the specified saved kit. |
| `/pk delete <name>` | Permanently deletes a saved kit from storage. |
| `/pk list` | Displays a list of all currently available kits. |

---

## Technical Details

* **Platform**: Fabric Loader (Minecraft 26.3 / *Wilderness Bound*)
* **API Dependencies**: Fabric API
* **Storage Path**: `<game-directory>/config/pvpkits/<player-uuid>.dat`

---

## Installation

1. Ensure you have **Fabric Loader** and **Fabric API** installed for Minecraft 26.3.
2. Download the latest `pvpkits.jar` release.
3. Drop the file into your server's or client's `mods` folder.
4. Launch the game!
