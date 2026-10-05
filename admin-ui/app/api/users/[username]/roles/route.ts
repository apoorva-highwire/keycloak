import { NextRequest, NextResponse } from "next/server";
import { addRoleToUser, IamApiError } from "@/lib/iam";

export async function POST(req: NextRequest, { params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  const { roleName } = await req.json();
  try {
    await addRoleToUser(username, roleName);
    return NextResponse.json({ ok: true });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
