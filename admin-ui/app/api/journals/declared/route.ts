import { NextRequest, NextResponse } from "next/server";
import { getRealmJournals, IamApiError, setRealmJournals } from "@/lib/iam";

export async function GET() {
  try {
    return NextResponse.json(await getRealmJournals());
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}

export async function PUT(req: NextRequest) {
  const { journals } = await req.json();
  try {
    await setRealmJournals(journals);
    return NextResponse.json({ ok: true });
  } catch (e) {
    if (e instanceof IamApiError) return NextResponse.json(e.body, { status: e.status });
    return NextResponse.json({ message: "Unknown error" }, { status: 502 });
  }
}
