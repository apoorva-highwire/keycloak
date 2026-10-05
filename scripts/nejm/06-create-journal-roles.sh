#!/usr/bin/env bash
# One read/write role PER journal (e.g. "nejm-core-read"), not one
# shared "journal-read" role across every journal — a single shared
# role would let anyone holding it read/write EVERY journal's resource
# once 09-create-journal-authorization.sh ties roles to resources,
# since a role-based policy only checks "does the caller hold this
# role", not which journal they're asking about. Per-journal roles are
# what actually makes 08/09's per-journal resources enforce anything.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")" && source common.sh

create_role_if_missing() {
  local role_name="$1"
  local description="$2"
  local existing
  existing=$(kc get roles/"$role_name" -r "$REALM_NAME" --fields name 2>/dev/null || true)
  if [ -n "$existing" ]; then
    echo "[06] Role '$role_name' already exists in '$REALM_NAME'."
  else
    echo "[06] Creating role '$role_name' in '$REALM_NAME' ..."
    kc create roles -r "$REALM_NAME" -s name="$role_name" -s description="$description"
  fi
}

for journal in "${JOURNALS[@]}"; do
  create_role_if_missing "${journal}-read" "Read access to $journal's content."
  create_role_if_missing "${journal}-write" "Submit/manage $journal's content."
done

echo "[06] Done. Roles for: ${JOURNALS[*]}"
