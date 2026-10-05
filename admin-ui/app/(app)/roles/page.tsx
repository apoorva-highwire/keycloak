"use client";

import { useEffect, useState } from "react";
import { KeyRound, Plus, ShieldCheck, X } from "lucide-react";
import {
  Banner,
  Button,
  Card,
  EmptyState,
  Field,
  inputClass,
  Modal,
  PageHeader,
  Pill,
  Spinner,
} from "@/components/ui";
import type { PermissionSummary, ResourceSummary, RoleSummary } from "@/lib/iam";

export default function RolesPage() {
  const [roles, setRoles] = useState<RoleSummary[] | null>(null);
  const [resources, setResources] = useState<ResourceSummary[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [selected, setSelected] = useState<string | null>(null);
  const [permissions, setPermissions] = useState<PermissionSummary[] | null>(null);
  const [grantResource, setGrantResource] = useState("");
  const [grantScope, setGrantScope] = useState("");
  const [busy, setBusy] = useState(false);

  const load = () => fetch("/api/roles").then((r) => r.json()).then(setRoles);

  useEffect(() => {
    load();
    fetch("/api/resources").then((r) => r.json()).then(setResources);
  }, []);

  function selectRole(roleName: string) {
    setSelected(roleName);
    setPermissions(null);
    setGrantResource("");
    setGrantScope("");
    fetch(`/api/roles/${encodeURIComponent(roleName)}/permissions`)
      .then((r) => r.json())
      .then(setPermissions);
  }

  async function refreshPermissions() {
    if (!selected) return;
    const res = await fetch(`/api/roles/${encodeURIComponent(selected)}/permissions`);
    setPermissions(await res.json());
  }

  async function grant() {
    if (!selected || !grantResource || !grantScope) return;
    setBusy(true);
    await fetch(`/api/roles/${encodeURIComponent(selected)}/permissions`, {
      method: "POST",
      body: JSON.stringify({ resourceName: grantResource, scope: grantScope }),
    });
    setGrantResource("");
    setGrantScope("");
    await refreshPermissions();
    setBusy(false);
  }

  async function revoke(permissionName: string) {
    setBusy(true);
    await fetch(`/api/permissions/${encodeURIComponent(permissionName)}`, { method: "DELETE" });
    await refreshPermissions();
    setBusy(false);
  }

  async function submitNewRole() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch("/api/roles", { method: "POST", body: JSON.stringify({ name, description }) });
      if (!res.ok) throw new Error((await res.json()).message ?? "Could not create role");
      setModalOpen(false);
      setName("");
      setDescription("");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setSubmitting(false);
    }
  }

  const grantResourceScopes = resources.find((r) => r.name === grantResource)?.scopes ?? [];

  return (
    <div className="px-10 py-10">
      <PageHeader
        title="Roles"
        description="Click a role to see how many permissions it grants, revoke one, or grant access to a new resource#scope."
        action={
          <Button icon={Plus} onClick={() => setModalOpen(true)}>
            New role
          </Button>
        }
      />

      <div className="grid grid-cols-[320px_1fr] gap-6">
        <Card className="overflow-hidden self-start">
          {roles === null ? (
            <Spinner />
          ) : roles.length === 0 ? (
            <EmptyState icon={KeyRound} title="No roles" />
          ) : (
            <ul>
              {roles.map((r) => (
                <li key={r.name}>
                  <button
                    onClick={() => selectRole(r.name)}
                    className={`w-full flex items-center gap-2.5 px-4 py-3 text-left text-sm border-b border-border last:border-0 transition-colors ${
                      selected === r.name ? "bg-accent-soft text-accent" : "hover:bg-surface-muted text-fg"
                    }`}
                  >
                    <KeyRound size={15} className={selected === r.name ? "text-accent" : "text-fg-muted"} />
                    <span className="truncate">{r.name}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <div>
          {!selected ? (
            <Card className="p-10">
              <EmptyState icon={ShieldCheck} title="Select a role" description="Pick one on the left to see what it grants access to." />
            </Card>
          ) : permissions === null ? (
            <Spinner />
          ) : (
            <Card className="p-6">
              <div className="flex items-center justify-between mb-5">
                <div className="flex items-center gap-2.5">
                  <h3 className="font-display text-lg text-fg">{selected}</h3>
                  <Pill tone={permissions.length > 0 ? "good" : "neutral"}>
                    {permissions.length} permission{permissions.length === 1 ? "" : "s"}
                  </Pill>
                </div>
                {busy && <span className="text-xs text-fg-muted">saving…</span>}
              </div>

              {permissions.length === 0 ? (
                <p className="text-sm text-fg-muted mb-5">
                  This role doesn&apos;t grant access to anything yet — not attached to any permission.
                </p>
              ) : (
                <div className="mb-5 overflow-hidden rounded-lg border border-border">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border bg-surface-muted text-left text-xs uppercase tracking-wide text-fg-muted">
                        <th className="px-4 py-2.5 font-medium">Resource#scope</th>
                        <th className="px-4 py-2.5 font-medium">Permission</th>
                        <th className="px-4 py-2.5 font-medium"></th>
                      </tr>
                    </thead>
                    <tbody>
                      {permissions.map((p) => (
                        <tr key={p.name} className="border-b border-border last:border-0">
                          <td className="px-4 py-2.5 tabular">
                            {p.resourceName}#{p.scopes.join(",")}
                          </td>
                          <td className="px-4 py-2.5 text-fg-muted">{p.name}</td>
                          <td className="px-4 py-2.5 text-right">
                            <button
                              onClick={() => revoke(p.name)}
                              className="inline-flex items-center gap-1 text-xs text-bad hover:underline"
                            >
                              <X size={12} />
                              Revoke
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              <section>
                <p className="text-xs font-medium uppercase tracking-wide text-fg-muted mb-2">Grant access to</p>
                <div className="flex gap-2">
                  <select
                    className={inputClass + " max-w-[180px]"}
                    value={grantResource}
                    onChange={(e) => {
                      setGrantResource(e.target.value);
                      setGrantScope("");
                    }}
                  >
                    <option value="">Resource…</option>
                    {resources.map((r) => (
                      <option key={r.name} value={r.name}>
                        {r.name}
                      </option>
                    ))}
                  </select>
                  <select
                    className={inputClass + " max-w-[140px]"}
                    value={grantScope}
                    onChange={(e) => setGrantScope(e.target.value)}
                    disabled={!grantResource}
                  >
                    <option value="">Scope…</option>
                    {grantResourceScopes.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                  <Button variant="secondary" onClick={grant} disabled={!grantResource || !grantScope}>
                    Grant
                  </Button>
                </div>
              </section>
            </Card>
          )}
        </div>
      </div>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="New role">
        {error && <Banner tone="bad">{error}</Banner>}
        <Field label="Name">
          <input className={inputClass} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. nejm-insight-read" />
        </Field>
        <Field label="Description">
          <input className={inputClass} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Optional" />
        </Field>
        <div className="flex justify-end gap-2 mt-5">
          <Button variant="secondary" onClick={() => setModalOpen(false)}>
            Cancel
          </Button>
          <Button onClick={submitNewRole} disabled={!name || submitting}>
            {submitting ? "Creating…" : "Create role"}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
