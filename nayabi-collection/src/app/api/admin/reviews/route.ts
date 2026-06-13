import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin-auth";
import { db } from "@/lib/db";

export async function GET(req: NextRequest) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const approved = req.nextUrl.searchParams.get("approved");
  const where =
    approved === "true" ? { isApproved: true } :
    approved === "false" ? { isApproved: false } :
    {};

  const reviews = await db.review.findMany({
    where,
    include: {
      product: { select: { name: true, slug: true } },
      user: { select: { name: true, email: true } },
    },
    orderBy: { createdAt: "desc" },
    take: 50,
  });

  return NextResponse.json(reviews);
}

export async function PATCH(req: NextRequest) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const { reviewId, approve } = await req.json();
  if (!reviewId || typeof approve !== "boolean") {
    return NextResponse.json({ error: "Invalid body" }, { status: 400 });
  }

  await db.review.update({
    where: { id: reviewId },
    data: { isApproved: approve },
  });

  return NextResponse.json({ ok: true });
}
