"use client";

import { useState } from "react";
import { Check, Copy, Info } from "lucide-react";
import { Modal, Pill } from "@/components/ui";

interface Permission {
  name: string;
  resourceName: string | null;
  scopes: string[];
}

interface Me {
  username: string;
  journalCode: string | null;
  scope: "JOURNAL" | "PUBLISHER";
  roles: string[];
  permissions: Permission[];
  accessToken: string;
  expiresAt: number;
  claims: Record<string, unknown>;
}

/**
 * Shows exactly what this login's own access token carries — the same
 * thing iam-service's CallerResolvingFilter reads to decide
 * JOURNAL/PUBLISHER/SERVICE scope, visible here instead of trusting that
 * it's right. The token is exposed to client JS on purpose for this one
 * "copy it and verify in curl/jwt.io" panel — a real deployment would
 * NOT want to do this routinely (an httpOnly cookie exists precisely so
 * client-side JS, and therefore any XSS, can't read it); this is a
 * deliberate, scoped exception for a demo console, not a pattern to
 * repeat elsewhere.
 */
export function LoginInfoButton() {
  const [open, setOpen] = useState(false);
  const [me, setMe] = useState<Me | null>(null);
  const [copied, setCopied] = useState(false);

  async function show() {
    setOpen(true);
    const res = await fetch("/api/me");
    if (res.ok) {
      setMe(await res.json());
    }
  }

  async function copyToken() {
    if (!me) return;
    await navigator.clipboard.writeText(me.accessToken);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return (
    <>
      <button
        onClick={show}
        className="flex items-center gap-1.5 rounded-md px-2 py-1 text-xs text-sidebar-fg-muted hover:bg-white/10 hover:text-sidebar-fg transition-colors"
      >
        <Info size={13} strokeWidth={2} />
        Login info
      </button>

      <Modal open={open} onClose={() => setOpen(false)} title="This login's token">
        {!me ? (
          <p className="text-sm text-fg-muted">Loading…</p>
        ) : (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-fg">{me.username}</span>
              <Pill tone={me.scope === "JOURNAL" ? "accent" : "good"}>{me.scope}</Pill>
              {me.journalCode && <Pill tone="neutral">{me.journalCode}</Pill>}
            </div>

            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-fg-muted mb-1.5">
                My content permissions{" "}
                <span className="text-fg-muted/70">
                  (from role{me.roles.length === 1 ? "" : "s"}: {me.roles.length ? me.roles.join(", ") : "none"})
                </span>
              </p>
              {me.permissions.length === 0 ? (
                <p className="text-sm text-fg-muted">
                  None — {me.scope === "PUBLISHER" ? "publisher-admin is an administrative role, not a content-access one." : "this login's roles don't grant any resource access."}
                </p>
              ) : (
                <div className="flex flex-wrap gap-1.5">
                  {me.permissions.map((p) => (
                    <Pill key={p.name} tone="accent">
                      {p.resourceName}#{p.scopes.join(",")}
                    </Pill>
                  ))}
                </div>
              )}
            </div>

            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-fg-muted mb-1.5">
                Access token — paste into jwt.io or use directly in curl
              </p>
              <div className="relative">
                <textarea
                  readOnly
                  value={me.accessToken}
                  rows={4}
                  className="w-full rounded-lg border border-border bg-surface-muted px-3 py-2 text-xs font-mono text-fg resize-none"
                  onClick={(e) => (e.target as HTMLTextAreaElement).select()}
                />
                <button
                  onClick={copyToken}
                  className="absolute top-2 right-2 flex items-center gap-1 rounded-md bg-surface px-2 py-1 text-xs text-fg-muted border border-border hover:text-fg"
                >
                  {copied ? <Check size={12} /> : <Copy size={12} />}
                  {copied ? "Copied" : "Copy"}
                </button>
              </div>
              <p className="mt-1 text-xs text-fg-muted">
                Expires {new Date(me.expiresAt).toLocaleTimeString()} — this is the SAME token every API call this
                console makes uses; iam-service re-verifies its signature on every request.
              </p>
            </div>

            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-fg-muted mb-1.5">Decoded claims</p>
              <pre className="rounded-lg border border-border bg-surface-muted px-3 py-2 text-xs font-mono text-fg overflow-x-auto max-h-48">
                {JSON.stringify(me.claims, null, 2)}
              </pre>
            </div>
          </div>
        )}
      </Modal>
    </>
  );
}
