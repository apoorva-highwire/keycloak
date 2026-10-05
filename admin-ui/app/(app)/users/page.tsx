"use client";

import { useEffect, useState } from "react";
import { ShieldCheck, User as UserIcon, Users, X } from "lucide-react";
import { Banner, Button, Card, EmptyState, inputClass, PageHeader, Pill, Spinner } from "@/components/ui";
import type { GroupSummary, RoleSummary, UserAccess, UserSummary } from "@/lib/iam";

export default function UsersPage() {
  const [users, setUsers] = useState<UserSummary[] | null>(null);
  const [groups, setGroups] = useState<GroupSummary[]>([]);
  const [roles, setRoles] = useState<RoleSummary[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [access, setAccess] = useState<UserAccess | null>(null);
  const [accessError, setAccessError] = useState<string | null>(null);
  const [pendingGroup, setPendingGroup] = useState("");
  const [pendingRole, setPendingRole] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    fetch("/api/users").then((r) => r.json()).then(setUsers);
    fetch("/api/groups").then((r) => r.json()).then(setGroups);
    fetch("/api/roles").then((r) => r.json()).then(setRoles);
  }, []);

  function loadAccess(username: string) {
    setSelected(username);
    setAccess(null);
    setAccessError(null);
    fetchAccess(username);
  }

  async function fetchAccess(username: string) {
    const res = await fetch(`/api/users/${encodeURIComponent(username)}/access`);
    const body = await res.json();
    if (!res.ok) {
      setAccess(null);
      setAccessError(
        body?.message ?? (res.status === 403 ? "Not permitted for this login." : "Could not load this user's access."),
      );
      return;
    }
    setAccessError(null);
    setAccess(body);
  }

  async function addGroup() {
    if (!selected || !pendingGroup) return;
    setBusy(true);
    const res = await fetch(`/api/users/${encodeURIComponent(selected)}/groups`, {
      method: "POST",
      body: JSON.stringify({ groupName: pendingGroup }),
    });
    if (res.ok) {
      setPendingGroup("");
      await refreshAccess();
    } else {
      const body = await res.json().catch(() => null);
      setAccessError(body?.message ?? "Could not add to that group.");
    }
    setBusy(false);
  }

  async function removeGroup(groupName: string) {
    if (!selected) return;
    setBusy(true);
    const res = await fetch(`/api/users/${encodeURIComponent(selected)}/groups?groupName=${encodeURIComponent(groupName)}`, {
      method: "DELETE",
    });
    if (res.ok) {
      await refreshAccess();
    } else {
      const body = await res.json().catch(() => null);
      setAccessError(body?.message ?? "Could not remove that group.");
    }
    setBusy(false);
  }

  async function addRole() {
    if (!selected || !pendingRole) return;
    setBusy(true);
    const res = await fetch(`/api/users/${encodeURIComponent(selected)}/roles`, {
      method: "POST",
      body: JSON.stringify({ roleName: pendingRole }),
    });
    if (res.ok) {
      setPendingRole("");
      await refreshAccess();
    } else {
      const body = await res.json().catch(() => null);
      setAccessError(body?.message ?? "Could not assign that role.");
    }
    setBusy(false);
  }

  async function refreshAccess() {
    if (!selected) return;
    await fetchAccess(selected);
  }

  return (
    <div className="px-10 py-10">
      <PageHeader
        title="Users"
        description="Groups are the intended path — join one, inherit every role it bundles. Direct roles are the exception for a one-off that doesn't warrant a whole group."
      />

      <div className="grid grid-cols-[320px_1fr] gap-6">
        <Card className="overflow-hidden self-start">
          {users === null ? (
            <Spinner />
          ) : users.length === 0 ? (
            <EmptyState icon={Users} title="No users" />
          ) : (
            <ul>
              {users.map((u) => (
                <li key={u.username}>
                  <button
                    onClick={() => loadAccess(u.username)}
                    className={`w-full flex items-center gap-2.5 px-4 py-3 text-left text-sm border-b border-border last:border-0 transition-colors ${
                      selected === u.username ? "bg-accent-soft text-accent" : "hover:bg-surface-muted text-fg"
                    }`}
                  >
                    <UserIcon size={15} className={selected === u.username ? "text-accent" : "text-fg-muted"} />
                    {u.username}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <div>
          {!selected ? (
            <Card className="p-10">
              <EmptyState icon={ShieldCheck} title="Select a user" description="Pick someone on the left to see and edit their groups and roles." />
            </Card>
          ) : accessError ? (
            <Card className="p-6">
              <Banner tone="bad">{accessError}</Banner>
              <p className="text-sm text-fg-muted">
                Group/role assignment is a publisher-level action — a journal-scoped login can&apos;t view or
                change it for any user, including itself.
              </p>
            </Card>
          ) : access === null ? (
            <Spinner />
          ) : (
            <Card className="p-6">
              <div className="flex items-center justify-between mb-5">
                <h3 className="font-display text-lg text-fg">{access.username}</h3>
                {busy && <span className="text-xs text-fg-muted">saving…</span>}
              </div>

              {accessError && <Banner tone="bad">{accessError}</Banner>}

              <section className="mb-5">
                <p className="text-xs font-medium uppercase tracking-wide text-fg-muted mb-2">Groups</p>
                <div className="flex flex-wrap gap-1.5 mb-2.5">
                  {access.groups.length === 0 ? (
                    <span className="text-sm text-fg-muted">Not in any group.</span>
                  ) : (
                    access.groups.map((g) => (
                      <span key={g} className="inline-flex items-center gap-1 rounded-full bg-good-soft text-good pl-2.5 pr-1.5 py-0.5 text-xs font-medium">
                        {g}
                        <button onClick={() => removeGroup(g)} className="rounded-full p-0.5 hover:bg-good/15">
                          <X size={11} />
                        </button>
                      </span>
                    ))
                  )}
                </div>
                <div className="flex gap-2">
                  <select className={inputClass + " max-w-xs"} value={pendingGroup} onChange={(e) => setPendingGroup(e.target.value)}>
                    <option value="">Add to a group…</option>
                    {groups.filter((g) => !access.groups.includes(g.name)).map((g) => (
                      <option key={g.name} value={g.name}>
                        {g.name}
                      </option>
                    ))}
                  </select>
                  <Button variant="secondary" onClick={addGroup} disabled={!pendingGroup}>
                    Add
                  </Button>
                </div>
              </section>

              <section className="mb-5">
                <p className="text-xs font-medium uppercase tracking-wide text-fg-muted mb-2">Direct roles</p>
                <div className="flex flex-wrap gap-1.5 mb-2.5">
                  {access.directRoles.length === 0 ? (
                    <span className="text-sm text-fg-muted">None.</span>
                  ) : (
                    access.directRoles.map((r) => <Pill key={r}>{r}</Pill>)
                  )}
                </div>
                <div className="flex gap-2">
                  <select className={inputClass + " max-w-xs"} value={pendingRole} onChange={(e) => setPendingRole(e.target.value)}>
                    <option value="">Assign a role directly…</option>
                    {roles.filter((r) => !access.directRoles.includes(r.name)).map((r) => (
                      <option key={r.name} value={r.name}>
                        {r.name}
                      </option>
                    ))}
                  </select>
                  <Button variant="secondary" onClick={addRole} disabled={!pendingRole}>
                    Add
                  </Button>
                </div>
              </section>

              <section>
                <p className="text-xs font-medium uppercase tracking-wide text-fg-muted mb-2">
                  Effective roles <span className="text-fg-muted/70">(direct + inherited from groups)</span>
                </p>
                <div className="flex flex-wrap gap-1.5">
                  {access.effectiveRoles.map((r) => (
                    <Pill key={r} tone="accent">
                      {r}
                    </Pill>
                  ))}
                </div>
              </section>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
