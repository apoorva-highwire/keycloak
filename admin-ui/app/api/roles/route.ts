import { NextRequest, NextResponse } from "next/server";
import { createRole, IamApiError, listRoles } from "@/lib/iam";

export async function GET() {
  try {
    return NextResponse.json(await listRoles());
  } catch (e) {
    return NextResponse.json({ message: e instanceof Error ? e.message : "Unknown error" }, { status: 502 });
  }
}

export async function POST(req: NextRequest) {
  const { name, description } = await req.json();
  try {
    await createRole(name, description ?? "");
    return NextResponse.json({ ok: true }, { status: 201 });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
