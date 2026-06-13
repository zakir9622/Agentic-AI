import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin-auth";
import { db } from "@/lib/db";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const { id } = await params;
  const order = await db.order.findUnique({
    where: { id },
    include: {
      user: { select: { name: true, email: true } },
      address: true,
      items: {
        select: {
          id: true,
          productName: true,
          variantDetails: true,
          quantity: true,
          unitPrice: true,
          totalPrice: true,
          imageUrl: true,
        },
      },
      shipment: {
        select: { trackingNumber: true, carrier: true, trackingUrl: true, status: true },
      },
      payment: {
        select: { razorpayPaymentId: true, method: true, status: true },
      },
    },
  });

  if (!order) return NextResponse.json({ error: "Not found" }, { status: 404 });
  return NextResponse.json(order);
}
