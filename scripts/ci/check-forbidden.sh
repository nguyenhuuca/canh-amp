#!/usr/bin/env bash
# Guard checks: banned words in sources and forbidden permissions in the merged manifest.
set -euo pipefail

cd "$(dirname "$0")/../.."
fail=0

# 1. Banned words (case-insensitive)
targets=()
for t in app/src/main app/build.gradle.kts settings.gradle.kts; do
  if [ -e "$t" ]; then targets+=("$t"); fi
done
if matches=$(grep -rniE 'winamp|nullsoft|llama' "${targets[@]}"); then
  echo "check-forbidden: banned words found:"
  echo "$matches"
  fail=1
fi

# 2. Forbidden permissions in the merged manifest (prefer debug, else release)
manifest=""
for variant in debug release; do
  manifest=$(find app/build/intermediates -name AndroidManifest.xml -path "*merged_manifest*/$variant/*" 2>/dev/null | head -n1 || true)
  if [ -n "$manifest" ]; then break; fi
done
if [ -z "$manifest" ]; then
  echo "check-forbidden: no merged manifest found under app/build/intermediates: build first"
  exit 1
fi
if perms=$(grep -nE 'android\.permission\.(INTERNET|RECORD_AUDIO|MODIFY_AUDIO_SETTINGS)' "$manifest"); then
  echo "check-forbidden: forbidden permissions in $manifest:"
  echo "$perms"
  fail=1
fi

if [ "$fail" -ne 0 ]; then exit 1; fi
echo "check-forbidden: OK"
