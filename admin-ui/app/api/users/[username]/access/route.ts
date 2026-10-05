import { NextResponse } from "next/server";
import { getUserAccess, IamApiError } from "@/lib/iam";

export async function GET(_req: Request, { params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  try {
    return NextResponse.json(await getUserAccess(username));
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
