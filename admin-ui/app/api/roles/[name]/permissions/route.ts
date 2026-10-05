import { NextRequest, NextResponse } from "next/server";
import { grantPermissionToRole, IamApiError, listPermissionsForRole } from "@/lib/iam";

export async function GET(_req: NextRequest, { params }: { params: Promise<{ name: string }> }) {
  const { name } = await params;
  try {
    return NextResponse.json(await listPermissionsForRole(name));
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}

export async function POST(req: NextRequest, { params }: { params: Promise<{ name: string }> }) {
  const { name } = await params;
  const { resourceName, scope } = await req.json();
  try {
    await grantPermissionToRole(name, resourceName, scope);
    return NextResponse.json({ ok: true }, { status: 201 });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
