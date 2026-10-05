"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { BookOpenText, KeyRound, LayoutGrid, LogOut, ShieldCheck, Users, UsersRound } from "lucide-react";
import { LoginInfoButton } from "@/components/LoginInfoButton";

const NAV = [
  { href: "/", label: "Overview", icon: LayoutGrid, adminOnly: false },
  { href: "/journals", label: "Journals", icon: BookOpenText, adminOnly: false },
  { href: "/roles", label: "Roles", icon: KeyRound, adminOnly: true },
  { href: "/groups", label: "Groups", icon: UsersRound, adminOnly: true },
  { href: "/users", label: "Users", icon: Users, adminOnly: true },
  { href: "/authorization", label: "Authorization", icon: ShieldCheck, adminOnly: false },
];

export function Sidebar({
  publisherName,
  realm,
  username,
  scope,
}: {
  publisherName: string;
  realm: string;
  username: string;
  scope: "JOURNAL" | "PUBLISHER";
}) {
  const pathname = usePathname();
  // Menu visibility only — NOT the actual security boundary. Roles/
  // Groups/Users administer who gets what content permission; a
  // JOURNAL-scoped login (a reader/journal account, not an admin) has
  // no real use for them, and UserController's own requireAdminCaller
  // already 403s the one truly sensitive action (assigning group/role
  // to a user) regardless of what this hides. Hiding them here just
  // means a journal login's sidebar matches what it can actually do,
  // instead of linking to pages that mostly error for it.
  const nav = NAV.filter((item) => !item.adminOnly || scope === "PUBLISHER");

  return (
    <aside className="w-64 shrink-0 bg-sidebar-bg text-sidebar-fg flex flex-col">
      <div className="px-5 py-6 border-b border-white/10">
        <p className="font-display text-lg leading-tight">{publisherName}</p>
        <div className="mt-1.5 flex items-center gap-1.5">
          <p className="text-xs uppercase tracking-wider text-sidebar-fg-muted">
            realm <span className="tabular">{realm}</span>
          </p>
          <span
            className={`rounded-full px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide ${
              scope === "PUBLISHER" ? "bg-good-soft text-good" : "bg-accent-soft text-accent"
            }`}
          >
            {scope}
          </span>
        </div>
      </div>

      <nav className="flex-1 px-3 py-4 space-y-1">
        {nav.map(({ href, label, icon: Icon }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors ${
                active
                  ? "bg-sidebar-active text-white font-medium"
                  : "text-sidebar-fg-muted hover:bg-white/5 hover:text-sidebar-fg"
              }`}
            >
              <Icon size={17} strokeWidth={2} />
              {label}
            </Link>
          );
        })}
      </nav>

      <div className="px-5 py-4 border-t border-white/10">
        <div className="flex items-center justify-between gap-2">
          <p className="text-sm text-sidebar-fg truncate" title={username}>
            {username}
          </p>
          <a
            href="/api/auth/logout"
            className="flex items-center gap-1.5 rounded-md px-2 py-1 text-xs text-sidebar-fg-muted hover:bg-white/10 hover:text-sidebar-fg transition-colors shrink-0"
          >
            <LogOut size={13} strokeWidth={2} />
            Log out
          </a>
        </div>
        <div className="mt-2 flex items-center justify-between">
          <p className="text-xs text-sidebar-fg-muted">via iam-service, as this user</p>
          <LoginInfoButton />
        </div>
      </div>
    </aside>
  );
}
