import { redirect } from "next/navigation";
import { Sidebar } from "@/components/Sidebar";
import { PUBLISHER_DISPLAY_NAME, REALM } from "@/lib/config";
import { getCallerJournal, readSession } from "@/lib/session";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  // Belt-and-suspenders — middleware already enforces this for every
  // request, but a Server Component rendering with no session at all
  // (e.g. the cookie was deleted mid-render) should still never show
  // the shell instead of silently rendering with a blank username.
  const session = await readSession();
  if (!session) {
    redirect("/login");
  }

  // Menu visibility, not a security boundary — see Sidebar's own
  // comment. The real restriction (UserController's requireAdminCaller)
  // is enforced server-side regardless of what this hides.
  const scope: "JOURNAL" | "PUBLISHER" = getCallerJournal(session) ? "JOURNAL" : "PUBLISHER";

  return (
    <div className="flex min-h-screen">
      <Sidebar publisherName={PUBLISHER_DISPLAY_NAME} realm={REALM} username={session.username} scope={scope} />
      <main className="flex-1 min-w-0">{children}</main>
    </div>
  );
}
