#!/usr/bin/env bash
# Runs every numbered script in this folder in order. Each one is
# idempotent (checks "does this already exist" before creating), so
# re-running the whole chain after some steps already ran is safe —
# it just confirms what's there and keeps going, never stops the chain
# because a step had nothing new to do.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

for script in 0*.sh; do
  echo "=== Running $script ==="
  bash "$script"
  echo ""
done

echo "All steps complete for realm '$(basename "$(pwd)")'."
