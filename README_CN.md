[English](README.md) | **中文**

# ElytraPlus 鞘翅飞行插件／模组

提供 Bukkit 插件和独立 Fabric 模组，共享飞行规则。穿上鞘翅，输入 `/fly`，就能像创造模式一样自由飞行，无需烟花。只有实际飞行时才会消耗耐久。

## Bukkit 安装

1. 从 [下载页面](https://github.com/Flock-YT/ElytraPlus/releases) 下载 `ElytraPlus-版本号.jar`。
2. 关闭服务端，将文件放入服务端的 `plugins` 文件夹，再启动服务端。
3. 穿上有耐久的鞘翅，使用 OP 账号在生存模式下体验。

适用于 Minecraft **1.9 至 26.3** 的 Bukkit、Spigot、Paper、Purpur 服务端，不支持 Folia。已进行自动兼容检查，尚未逐版本实服测试；建议先在测试服试用。

升级时先备份 `plugins/ElytraPlus` 文件夹，再停服替换旧插件文件。已有配置会保留。

## Fabric 安装与权限

Fabric 使用一个通用 `ElytraPlus-Fabric-<版本>.jar`，覆盖 **1.14～26.3 的全部 48 个锁定正式版**。安装 Fabric Loader **0.19.5 或更高版本**，将 JAR 放入 `mods`。核心无需 Fabric API；玩家连接远程服务器无需安装客户端模组。也支持本地单人和局域网集成服务器。

使用 `/fly` 开关飞行；命令冲突时使用 `/elytraplus:fly`。默认 OP 等级 2 及以上和单人房主可以使用并绕过世界限制，默认仍消耗耐久。普通局域网访客不自动获得权限。

可选安装适用于当前游戏版本的 [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) 和权限管理模组，使用下表相同的三个权限节点授权普通玩家。显式允许／拒绝优先；未配置时使用上述默认规则。权限扩展的依赖由对应模组提供，不包含在 ElytraPlus 内。

Fabric 配置位于 `config/elytraplus/config.yml`，双语文件位于其 `lang/` 目录。配置键、语言与耐久行为和 Bukkit 相同。世界列表使用 `minecraft:overworld`、`minecraft:the_nether`、`minecraft:the_end` 或自定义维度 ID（例如 `example:moon`），也兼容 `world`、`world_nether`、`world_the_end` 别名。Fabric 不上传 bStats 统计，保留开关只为兼容配置。

本次仅做代码测试、编译和离线钩子／打包校验；不运行真实游戏场景，不代表全版本游戏实测。详见 [Fabric 验证说明](fabric/README.md)。

## 怎么飞

1. 把鞘翅穿在胸甲栏，不能只放在背包里。
2. 在游戏中输入 `/fly` 开启飞行。
3. 像创造模式一样双击跳跃键起飞；默认按住空格上升、Shift 下降。
4. 落地后再次输入 `/fly`，即可关闭。

**鞘翅损坏、脱下鞘翅或进入禁飞世界时会停止飞行，可能摔伤。** 请留意耐久，安全落地后再关闭。损坏的鞘翅会保留，可以修复后继续使用。死亡或重新登录后，需要重新输入 `/fly`。

## 让普通玩家也能用

Bukkit 默认只有 OP 可以使用。请在你使用的权限插件中，给玩家或玩家组添加 `elytraplus.use` 权限；想让所有普通玩家都能用，就添加到默认玩家组。

| 权限名称 | 作用 | 默认谁有 |
| --- | --- | --- |
| `elytraplus.use` | 使用鞘翅飞行 | OP |
| `elytraplus.bypass.durability` | 本插件飞行不消耗耐久，仍需穿戴可用鞘翅 | 无人 |
| `elytraplus.bypass.world` | 在禁飞世界也能飞 | OP |

**测试世界限制时，请使用没有绕过权限的普通玩家账号。** OP 默认不受世界限制。

## 修改设置

Bukkit 配置文件在 `plugins/ElytraPlus/config.yml`，首次启动后自动生成。默认配置就能使用，按需修改即可。

- `true` 表示开启，`false` 表示关闭。
- 只改需要的值，保留每行开头的空格，不要用 Tab 缩进。
- 保存后重启服务端生效，本插件没有重载命令。

### 不想消耗耐久

找到 `durability`，把它下面的 `enabled: true` 改为 `enabled: false`。即使关闭消耗，也不能用已经损坏的鞘翅起飞。

默认会考虑鞘翅的「耐久」附魔，等级越高越省耐久。想自己设置消耗概率，可以看配置文件中 `custom-chance` 的注释。

### 禁止某些世界飞行

世界限制默认关闭（`type: false`）。

找到 `settings` 下面的 `world-list`，替换为以下内容，保留前面的空格。这个例子禁止在下界和末地使用：

```yaml
  world-list:
    type: BLACKLIST
    worlds:
      - "world_nether"
      - "world_the_end"
```

- 把世界名换成你服务器实际使用的名称，大小写必须一致，通常就是世界文件夹名。
- 想只允许列表中的世界飞行，把 `BLACKLIST` 改为 `WHITELIST`。
- 想允许所有世界，把 `type` 改为 `false`（即 `type: false`）。列表中的世界名不再参与限制，`worlds` 仍须保持列表格式；权限和鞘翅装备等其他条件仍然生效。

### 语言和提示文字

`language: zh_CN` 使用中文，改成 `language: en_US` 使用英文。

想修改游戏里的提示，打开 `plugins/ElytraPlus/lang/zh_CN.yml`（英文对应 `en_US.yml`），修改引号里的文字即可。支持 `&` 颜色代码；改成 `""` 可以隐藏该条提示。保存后重启生效。

从旧版升级时，原来写在 `config.yml` 的 `messages` 提示需要手动复制到对应语言文件中。

### 关闭使用统计

找到 `bstats`，把它下面的 `enabled: true` 改成 `enabled: false`，保存后重启即可。

## 构建与开发

项目使用 Gradle Wrapper，无需 Maven。`shared/` 是无平台依赖的 Java 8 核心；`bukkit/` 包含 Bukkit 入口和适配；`fabric/` 是独立的多版本构建。根 `gradle.properties` 是唯一版本来源。

```sh
# JDK 25 运行，安装 JDK 21 工具链；需要 Python 3
./gradlew --no-daemon build
# 单独构建 Bukkit：JDK 21 即可，输出 Java 8 字节码
./gradlew --no-daemon bukkitBuild
# 单独构建并验证全部 Fabric 版本：JDK 25
./gradlew --no-daemon fabricBuild
# 两端清理；也可使用 bukkitClean / fabricClean
./gradlew clean
# Bukkit 同一 JAR 的全 API 代码检查：使用 JDK 25
./scripts/verify-api-compatibility.sh
```

产物为 `bukkit/build/libs/ElytraPlus-<版本>.jar` 和 `fabric/build/libs/ElytraPlus-Fabric-<版本>.jar`。可使用 `:bukkit:build`／`:fabric:build` 及各自的 `clean` 任务；单平台构建无需配置另一平台的游戏模块。IDE 展示 `shared`、`bukkit`、`fabric` 三个同级节点。

单类测试使用 `./gradlew :bukkit:test --tests '*FlightManagerTest'`，共享核心测试使用 `./gradlew :shared:test`。Gradle 自动发现工具链；必要时通过 `-Porg.gradle.java.installations.paths=<jdk21>,<jdk25>` 指定。清理会删除整个 Fabric 构建目录。

CI 校验两端后发布同一批已验证的 JAR、SHA-256 和 Fabric 代码报告。仅版本变化或手动触发才发布；此次迁移保持版本 2.0.2。

## 常见问题

- **提示没有权限？** 请让管理员给你添加 `elytraplus.use` 权限。
- **提示开启了但没飞起来？** 开启后需要自己双击跳跃键，不会自动升空。
- **提示已经拥有飞行能力？** 创造、旁观模式或其他插件已经允许你飞行，无需再次开启。若有多个插件使用 `/fly`，可用 `/elytraplus:fly` 指定本插件的命令。
- **修改配置后插件不能用了？** 检查缩进和填写的值，服务端控制台会指出有问题的设置。也可以恢复备份后重启。

遇到其他问题，可以到 [问题反馈](https://github.com/Flock-YT/ElytraPlus/issues) 附上服务端版本、问题出现的步骤和报错信息。

[MIT 许可证](LICENSE)。

[![ElytraPlus 插件安装量统计图](https://bstats.org/signatures/bukkit/ElytraPlus.svg)](https://bstats.org/plugin/bukkit/ElytraPlus/34208)
