import { NextResponse } from "next/server";
import { listUsers } from "@/lib/iam";

export async function GET() {
  try {
    return NextResponse.json(await listUsers());
  } catch (e) {
    return NextResponse.json({ message: e instanceof Error ? e.message : "Unknown error" }, { status: 502 });
  }
}
