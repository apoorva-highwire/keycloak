import { NextResponse } from "next/server";
import { deletePermission, IamApiError } from "@/lib/iam";

export async function DELETE(_req: Request, { params }: { params: Promise<{ name: string }> }) {
  const { name } = await params;
  try {
    await deletePermission(name);
    return NextResponse.json({ ok: true });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
