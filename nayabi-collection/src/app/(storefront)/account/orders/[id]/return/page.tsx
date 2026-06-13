import { notFound, redirect } from "next/navigation";
import Link from "next/link";
import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { GlassCard } from "@/components/ui";
import { RETURN_WINDOW_DAYS } from "@/lib/constants";
import { ReturnForm } from "./return-form";
import { ArrowLeft } from "lucide-react";

export const metadata = { title: "Return / Exchange" };

export default async function ReturnPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const session = await auth();

  const order = await db.order.findFirst({
    where: { id, userId: session!.user.id },
    include: {
      items: {
        include: { product: { select: { slug: true } } },
      },
      returnRequests: { select: { id: true, status: true } },
    },
  });

  if (!order) notFound();

  if (order.orderStatus !== "DELIVERED") {
    redirect(`/account/orders/${id}`);
  }

  const returnWindowMs = RETURN_WINDOW_DAYS * 24 * 60 * 60 * 1000;
  // eslint-disable-next-line react-hooks/purity
  if (Date.now() - order.updatedAt.getTime() > returnWindowMs) {
    return (
      <GlassCard padding="lg" className="text-center">
        <p className="text-[var(--color-text-primary)] font-medium">Return window closed</p>
        <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
          Returns can only be requested within {RETURN_WINDOW_DAYS} days of delivery.
        </p>
        <Link
          href={`/account/orders/${id}`}
          className="mt-4 inline-block text-sm text-[var(--color-gold)] underline underline-offset-2"
        >
          Back to order
        </Link>
      </GlassCard>
    );
  }

  const activeReturn = order.returnRequests.find((r) => r.status !== "REJECTED");
  if (activeReturn) {
    redirect(`/account/orders/${id}`);
  }

  const orderItems = order.items.map((item) => ({
    id: item.id,
    variantId: item.variantId,
    productName: item.productName,
    variantDetails: item.variantDetails,
    imageUrl: item.imageUrl,
    quantity: item.quantity,
    totalPrice: item.totalPrice,
  }));

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Link
          href={`/account/orders/${id}`}
          className="inline-flex items-center gap-1.5 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-gold)] transition-colors"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />
          Back to order
        </Link>
        <h2 className="mt-3 text-xl font-semibold text-[var(--color-text-primary)]">
          Return / Exchange
        </h2>
        <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
          Order {order.orderNumber} · Return window closes in{" "}
          {RETURN_WINDOW_DAYS} days of delivery.
        </p>
      </div>

      <ReturnForm orderId={id} orderItems={orderItems} />
    </div>
  );
}
