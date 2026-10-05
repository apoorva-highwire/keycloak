import { NextRequest, NextResponse } from "next/server";
import { addRoleToGroup, IamApiError, removeRoleFromGroup } from "@/lib/iam";

export async function POST(req: NextRequest, { params }: { params: Promise<{ name: string }> }) {
  const { name } = await params;
  const { roleName } = await req.json();
  try {
    await addRoleToGroup(name, roleName);
    return NextResponse.json({ ok: true });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}

export async function DELETE(req: NextRequest, { params }: { params: Promise<{ name: string }> }) {
  const { name } = await params;
  const roleName = req.nextUrl.searchParams.get("roleName");
  if (!roleName) return NextResponse.json({ message: "roleName required" }, { status: 400 });
  try {
    await removeRoleFromGroup(name, roleName);
    return NextResponse.json({ ok: true });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
