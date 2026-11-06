# Fabric architecture and code verification

ElytraPlus ships one universal Fabric JAR for the 48 stable releases in `versions.lock.json`, from Minecraft 1.14 to 26.3. Acceptance is code-only: no Minecraft process or game behavior tests are run.

The root Gradle build exposes sibling `shared`, `bukkit`, and `fabric` projects. The lightweight Fabric lifecycle project delegates to an independent build so Bukkit work and IDE import do not configure game modules. Both builds read the root `gradle.properties` version. Shared outputs are isolated between builds.

The Java 8 shell bundles the platform-free shared core and privately relocated SnakeYAML. It depends on one `elytraplus_runtime` module. Each of the 48 nested candidates has a unique version and exact Minecraft dependency. Fabric Loader selects the matching candidate. The shell checks the selected version against the running game. Older native modules are remapped to intermediary; 26.x uses official names. Runtime bytecode follows each game's Java requirement. Loader 0.19.5 is the runtime minimum; the build uses 0.20.1. Fabric API is not a core dependency.

Native adapters and required Mixins are generated against the actual locked game classes. They handle abilities and their synchronization, live equipment and durability, Unbreaking and Unbreakable, dimensions, commands, death/respawn, disconnect, game mode and tick lifecycle. Equipment writes stop at maximum damage minus one and never delete the elytra. Server tick, command and player lifecycle calls run on the server thread. The abilities-packet hook runs at TAIL, after native packet thread dispatch. Required methods and fields must exist; no optional hooks or silent feature fallbacks are generated.

The optional permissions bridge looks up the public online `Permissions.check(source, node, boolean)` overload from the installed Fabric Permissions API. It uses that API's runtime parameter types, avoiding links to optional Minecraft-typed APIs in the Java 8 shell. No API implementation is bundled. Explicit allow/deny overrides the fallback; absent or undefined permissions use OP level 2 / integrated owner for use and world bypass, false for durability bypass. An incompatible installed API produces an error rather than granting permissions. Install the permissions API and a permission provider appropriate for your Minecraft version.

Configuration lives in `config/elytraplus/config.yml` and `config/elytraplus/lang/`. Both languages and all flight settings match Bukkit. Dimension IDs and legacy aliases are normalized only on Fabric. Invalid initial configuration aborts server startup. State belongs to each server instance and is removed on shutdown. A client connected to a remote server creates no local flight sessions. Fabric accepts the bStats setting but uploads no metrics.

From the repository root, using JDK 25 and Python 3:

```sh
./gradlew --no-daemon fabricBuild
# Equivalent independent build + final artifact check:
./gradlew --no-daemon -p fabric check shadowJar
python3 scripts/verify-fabric-code.py fabric/build/libs/ElytraPlus-Fabric-2.0.2.jar
```

A development subset is available with `-Ptargets=1.14,1.21.11,26.3`. Subsets cannot pass universal-artifact acceptance. The release check requires all 48 locked modules, successful unit-test evidence, matching module/report hashes, Java targets, exact metadata and platform isolation. `verify-native.gradle` checks native field/method references, injection descriptors, callback compatibility and registration in the final remapped JAR. Its negative fixtures reject broken native contracts. Python tests reject packaging errors and missing/failed test evidence.

`fabric/build/code-report.json` records code verification and the exact universal JAR SHA-256. Compile and static-check success do not establish real-server or client gameplay compatibility. No startup or game behavior pass is claimed.
