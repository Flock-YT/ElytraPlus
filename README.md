**English** | [中文](README_CN.md)

# ElytraPlus

Wear an elytra and use `/fly` to fly like in creative mode, without fireworks. Durability is consumed only while you are actually flying.

## Bukkit installation

1. Download `ElytraPlus-<version>.jar` from the [downloads page](https://github.com/Flock-YT/ElytraPlus/releases).
2. Stop your server, put the file in its `plugins` folder, then start the server.
3. Wear an elytra with durability remaining and try it in survival mode with an OP account.

Designed for Minecraft **1.9–26.3** on Bukkit, Spigot, Paper and Purpur. Folia is not supported. Automated compatibility checks have been run, but each version has not been tested on a live server; try it on a test server first.

When upgrading, back up `plugins/ElytraPlus`, then stop the server and replace the old plugin file. Your existing settings are kept.

## Fabric installation and permissions

One universal `ElytraPlus-Fabric-<version>.jar` covers all **48 locked stable releases from 1.14 through 26.3**. Install Fabric Loader **0.19.5 or newer** and put the JAR in `mods`. Fabric API is not required by the core. Remote players do not need a client mod; local single-player and LAN integrated servers are supported.

Use `/fly`, or `/elytraplus:fly` when another mod owns `/fly`. OP level 2+ and the integrated-server owner can use flight and bypass world restrictions by default. Durability bypass defaults to false. Ordinary LAN visitors do not automatically receive permissions.

Optionally install the [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) and a compatible permission manager for your game version. The same three permission nodes below apply. Explicit allow/deny overrides defaults; undefined permissions retain the defaults above. Dependencies of the optional permission extension are supplied by those mods, not bundled in ElytraPlus.

Fabric configuration lives in `config/elytraplus/config.yml`, with bilingual files under `config/elytraplus/lang/`. Settings and durability rules match Bukkit. World lists accept dimension IDs (`minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end`, or custom IDs such as `example:moon`) and the aliases `world`, `world_nether`, `world_the_end`. Fabric accepts the bStats switch for configuration compatibility but uploads no metrics.

Acceptance is code-only: unit tests, compilation and offline native-hook/package verification. No real game scenarios or game startup tests are run or claimed. See [Fabric verification](fabric/README.md).


## How to fly

1. Equip an elytra in your chest slot; keeping it in your inventory is not enough.
2. Enter `/fly` in game to enable flight.
3. Double-tap jump to take off, just like creative mode. With default controls, hold Space to rise and Shift to descend.
4. Land and enter `/fly` again to disable flight.

**Flight stops if your elytra breaks, you remove it, or you enter a world where flight is blocked. You may take fall damage.** Watch your durability and land before disabling flight. Broken elytra are kept and can be repaired. After dying or logging back in, use `/fly` again.

## Let regular players fly

Only OPs can use the plugin by default. In your permissions plugin, grant `elytraplus.use` to a player or group. To let all regular players use it, grant it to the default player group.

| Permission | What it allows | Who has it by default |
| --- | --- | --- |
| `elytraplus.use` | Use elytra flight | OPs |
| `elytraplus.bypass.durability` | Fly without this plugin consuming durability; a usable elytra is still required | Nobody |
| `elytraplus.bypass.world` | Fly even in blocked worlds | OPs |

**Test world restrictions with a regular player who has no bypass permission.** OPs bypass world restrictions by default.

## Settings

The plugin creates `plugins/ElytraPlus/config.yml` on first startup. The defaults are ready to use; change only what you need.

- `true` means on and `false` means off.
- Keep the spaces at the start of each line. Do not use Tab for indentation.
- Save and restart the server to apply changes. This plugin has no reload command.

### Disable durability consumption

Find `durability` and change the `enabled: true` beneath it to `enabled: false`. An already broken elytra still cannot be used to start flying.

By default, the Unbreaking enchantment reduces durability consumption: higher levels make the elytra last longer. To set your own damage chance, see the `custom-chance` comments in the configuration file.

### Block flight in certain worlds

World restrictions are disabled by default (`type: false`).

Find `world-list` under `settings` and replace it with the following, keeping the leading spaces. This example blocks flight in the Nether and the End:

```yaml
  world-list:
    type: BLACKLIST
    worlds:
      - "world_nether"
      - "world_the_end"
```

- Replace the world names with your server's actual names, including matching capitalization. These are usually the world folder names.
- To allow flight only in the listed worlds, change `BLACKLIST` to `WHITELIST`.
- To allow all worlds, set `type: false`. Listed world names no longer restrict flight, but `worlds` must still be a list. Permissions, elytra equipment, and other flight requirements still apply.

### Language and messages

Change `language: zh_CN` to `language: en_US` for English messages.

To edit in-game messages, open `plugins/ElytraPlus/lang/en_US.yml` (or `zh_CN.yml` for Chinese) and change the text inside the quotes. You can use `&` color codes or set a message to `""` to hide it. Save and restart to apply changes.

If you are upgrading from an older version, manually copy any custom `messages` from `config.yml` into the matching language file.

### Disable usage statistics

Find `bstats`, change the `enabled: true` beneath it to `enabled: false`, then save and restart.

## Building and development

Use the Gradle Wrapper; Maven is no longer needed. `shared/` is the platform-free Java 8 core; `bukkit/` contains Bukkit adapters; `fabric/` is an independent multi-version build. The root `gradle.properties` is the single version source.

```sh
# Run on JDK 25 with a JDK 21 toolchain installed; Python 3 is required
./gradlew --no-daemon build
# Bukkit only, JDK 21; production output remains Java 8
./gradlew --no-daemon bukkitBuild
# Fabric only, all locked versions, JDK 25
./gradlew --no-daemon fabricBuild
./gradlew clean
# Check the same Bukkit JAR against all APIs using JDK 25
./scripts/verify-api-compatibility.sh
```

Artifacts are `bukkit/build/libs/ElytraPlus-<version>.jar` and `fabric/build/libs/ElytraPlus-Fabric-<version>.jar`. Use `:bukkit:build` / `:fabric:build` and their `clean` tasks, or `bukkitClean` / `fabricClean`, for platform-specific work. IDE import exposes three sibling modules without configuring the game build. Cleaning removes all Fabric build outputs.

Run one regression class with `./gradlew :bukkit:test --tests '*FlightManagerTest'`, or shared tests with `./gradlew :shared:test`. Gradle discovers toolchains; use `-Porg.gradle.java.installations.paths=<jdk21>,<jdk25>` when needed. CI publishes the exact verified JARs, SHA-256 sidecars and Fabric code report after both platforms pass. Version changes or manual dispatch trigger releases; this migration keeps version 2.0.2.


## Common questions

- **No permission?** Ask an administrator to grant you `elytraplus.use`.
- **Flight is enabled but you are still on the ground?** Double-tap jump to take off. Enabling flight does not lift you automatically.
- **Already able to fly?** Creative mode, spectator mode or another plugin already lets you fly, so there is no need to enable it again. If multiple plugins use `/fly`, use `/elytraplus:fly` to select this plugin's command.
- **Plugin stopped working after a configuration edit?** Check indentation and values. The server console will identify the setting with a problem. You can also restore your backup and restart.

For other problems, [report an issue](https://github.com/Flock-YT/ElytraPlus/issues) with your server version, steps to reproduce the problem and any error messages.

[MIT license](LICENSE).

[![ElytraPlus Plugin Installation Chart](https://bstats.org/signatures/bukkit/ElytraPlus.svg)](https://bstats.org/plugin/bukkit/ElytraPlus/34208)
