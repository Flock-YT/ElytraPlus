#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
version="$(python3 scripts/project_version.py)"
artifact="$project_root/bukkit/build/libs/ElytraPlus-$version.jar"
if [[ ! -f "$artifact" || ! -f bukkit/build/classes/java/test/cn/ericcraft/elytraPlus/ApiSmoke.class ]]; then
  echo 'Run ./gradlew bukkitBuild first (the production JAR and harness must already exist).' >&2
  exit 1
fi
if (( $# == 0 )); then
  set -- 1.9 1.9.4 1.10.2 1.11.2 1.12.2 1.13.2 1.14.4 1.15.2 1.16.5 1.17.1 1.18.2 1.19.4 1.20.3 1.20.6 1.21 1.21.11 26.1.2 26.2 26.3
fi
mkdir -p build/compatibility
before="$(shasum -a 256 "$artifact")"
for api in "$@"; do
  [[ "$api" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]] || { echo "Invalid API version: $api" >&2; exit 1; }
  classpath_file="$project_root/build/compatibility/$api.classpath"
  ./gradlew --no-daemon -q :bukkit:writeCompatibilityClasspath "-PcompatibilityApi=$api"
  echo "Checking API $api with unchanged artifact"
  "${API_JAVA_HOME:-$JAVA_HOME}/bin/java" -Dnet.bytebuddy.experimental=true -cp "$artifact:$project_root/bukkit/build/classes/java/test:$(cat "$classpath_file")" \
    cn.ericcraft.elytraPlus.ApiSmoke "$artifact" 2>&1 | tee "build/compatibility/$api.log"
done
[[ "$before" == "$(shasum -a 256 "$artifact")" ]] || { echo 'Artifact changed during verification' >&2; exit 1; }
echo "$before" > build/compatibility/artifact.sha256
