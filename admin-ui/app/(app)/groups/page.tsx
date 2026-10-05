"use client";

import { useEffect, useState } from "react";
import { Plus, UsersRound, X } from "lucide-react";
import { Banner, Button, Card, EmptyState, Field, inputClass, Modal, PageHeader, Pill, Spinner } from "@/components/ui";
import type { GroupSummary, RoleSummary } from "@/lib/iam";

export default function GroupsPage() {
  const [groups, setGroups] = useState<GroupSummary[] | null>(null);
  const [roles, setRoles] = useState<RoleSummary[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [pendingRole, setPendingRole] = useState<Record<string, string>>({});

  const load = () => fetch("/api/groups").then((r) => r.json()).then(setGroups);

  useEffect(() => {
    load();
    fetch("/api/roles").then((r) => r.json()).then(setRoles);
  }, []);

  async function createGroup() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch("/api/groups", { method: "POST", body: JSON.stringify({ name }) });
      if (!res.ok) throw new Error((await res.json()).message ?? "Could not create group");
      setModalOpen(false);
      setName("");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setSubmitting(false);
    }
  }

  async function addRole(groupName: string) {
    const roleName = pendingRole[groupName];
    if (!roleName) return;
    await fetch(`/api/groups/${encodeURIComponent(groupName)}/roles`, {
      method: "POST",
      body: JSON.stringify({ roleName }),
    });
    setPendingRole((p) => ({ ...p, [groupName]: "" }));
    await load();
  }

  async function removeRole(groupName: string, roleName: string) {
    await fetch(`/api/groups/${encodeURIComponent(groupName)}/roles?roleName=${encodeURIComponent(roleName)}`, {
      method: "DELETE",
    });
    await load();
  }

  return (
    <div className="max-w-4xl px-10 py-10">
      <PageHeader
        title="Groups"
        description="Bundle several roles so a user is assigned once — join the group, inherit every role it carries. No limit on how many roles a group can hold."
        action={
          <Button icon={Plus} onClick={() => setModalOpen(true)}>
            New group
          </Button>
        }
      />

      {groups === null ? (
        <Spinner />
      ) : groups.length === 0 ? (
        <EmptyState icon={UsersRound} title="No groups yet" description="Create one, then bundle roles into it below." />
      ) : (
        <div className="space-y-4">
          {groups.map((g) => (
            <Card key={g.name} className="p-5">
              <div className="flex items-center justify-between mb-3">
                <p className="font-display text-base text-fg">{g.name}</p>
              </div>

              <div className="flex flex-wrap gap-1.5 mb-4">
                {g.roles.length === 0 ? (
                  <span className="text-sm text-fg-muted">No roles bundled yet.</span>
                ) : (
                  g.roles.map((r) => (
                    <span key={r} className="inline-flex items-center gap-1 rounded-full bg-accent-soft text-accent pl-2.5 pr-1.5 py-0.5 text-xs font-medium">
                      {r}
                      <button onClick={() => removeRole(g.name, r)} className="rounded-full p-0.5 hover:bg-accent/15">
                        <X size={11} />
                      </button>
                    </span>
                  ))
                )}
              </div>

              <div className="flex gap-2">
                <select
                  className={inputClass + " max-w-xs"}
                  value={pendingRole[g.name] ?? ""}
                  onChange={(e) => setPendingRole((p) => ({ ...p, [g.name]: e.target.value }))}
                >
                  <option value="">Add a role…</option>
                  {roles.filter((r) => !g.roles.includes(r.name)).map((r) => (
                    <option key={r.name} value={r.name}>
                      {r.name}
                    </option>
                  ))}
                </select>
                <Button variant="secondary" onClick={() => addRole(g.name)} disabled={!pendingRole[g.name]}>
                  Add
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="New group">
        {error && <Banner tone="bad">{error}</Banner>}
        <Field label="Name">
          <input className={inputClass} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. nejm-core-subscribers" />
        </Field>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setModalOpen(false)}>
            Cancel
          </Button>
          <Button onClick={createGroup} disabled={!name || submitting}>
            {submitting ? "Creating…" : "Create group"}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
