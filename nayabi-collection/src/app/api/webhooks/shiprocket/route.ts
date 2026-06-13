import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";

export async function POST(req: NextRequest) {
  try {
    const token = req.headers.get("x-shiprocket-token");
    if (process.env.SHIPROCKET_WEBHOOK_TOKEN && token !== process.env.SHIPROCKET_WEBHOOK_TOKEN) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await req.json();
    const awb = body.awb ?? body.awb_code;
    const status: string = body.current_status ?? body.status ?? "";
    const location: string = body.location ?? "";
    const activity: string = body.activity ?? status;

    if (!awb) return NextResponse.json({ ok: true });

    const shipment = await db.shipment.findFirst({
      where: { trackingNumber: awb },
      select: { id: true, orderId: true },
    });
    if (!shipment) return NextResponse.json({ ok: true });

    const normalised = status.toLowerCase();
    let orderStatus: string | null = null;
    if (normalised.includes("deliver")) orderStatus = "DELIVERED";
    else if (normalised.includes("transit") || normalised.includes("shipped")) orderStatus = "SHIPPED";

    await db.$transaction([
      db.shipmentEvent.create({
        data: {
          shipmentId: shipment.id,
          status,
          location,
          message: activity,
          timestamp: new Date(),
        },
      }),
      ...(orderStatus === "DELIVERED"
        ? [
            db.shipment.update({
              where: { id: shipment.id },
              data: { status: "DELIVERED", deliveredAt: new Date() },
            }),
            db.order.update({
              where: { id: shipment.orderId },
              data: { orderStatus: "DELIVERED" },
            }),
          ]
        : []),
    ]);

    return NextResponse.json({ ok: true });
  } catch (e) {
    console.error("Shiprocket webhook error:", e);
    return NextResponse.json({ error: "Internal error" }, { status: 500 });
  }
}
