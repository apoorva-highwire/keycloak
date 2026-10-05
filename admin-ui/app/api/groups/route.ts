import { NextRequest, NextResponse } from "next/server";
import { createGroup, IamApiError, listGroups } from "@/lib/iam";

export async function GET() {
  try {
    return NextResponse.json(await listGroups());
  } catch (e) {
    return NextResponse.json({ message: e instanceof Error ? e.message : "Unknown error" }, { status: 502 });
  }
}

export async function POST(req: NextRequest) {
  const { name } = await req.json();
  try {
    await createGroup(name);
    return NextResponse.json({ ok: true }, { status: 201 });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
