#!/usr/bin/env bash
# Logs kcadm.sh into master as the bootstrap admin. kcadm persists this
# session in the container's own config file, so every later script in
# this folder — run standalone or via run-all.sh — just works without
# separately passing a token around.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

echo "[01] Getting admin token (master realm, bootstrap admin) ..."
kc config credentials --server http://localhost:8080 --realm master \
  --user "$KC_ADMIN" --password "$KC_ADMIN_PASSWORD"
echo "[01] Done."
