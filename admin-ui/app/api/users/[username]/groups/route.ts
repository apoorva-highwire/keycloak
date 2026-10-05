import { NextRequest, NextResponse } from "next/server";
import { addUserToGroup, IamApiError, removeUserFromGroup } from "@/lib/iam";

export async function POST(req: NextRequest, { params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  const { groupName } = await req.json();
  try {
    await addUserToGroup(username, groupName);
    return NextResponse.json({ ok: true });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}

export async function DELETE(req: NextRequest, { params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  const groupName = req.nextUrl.searchParams.get("groupName");
  if (!groupName) return NextResponse.json({ message: "groupName required" }, { status: 400 });
  try {
    await removeUserFromGroup(username, groupName);
    return NextResponse.json({ ok: true });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
