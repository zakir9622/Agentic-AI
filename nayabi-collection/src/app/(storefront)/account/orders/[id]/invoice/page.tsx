import { notFound } from "next/navigation";
import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { formatPrice } from "@/lib/utils";
import { PrintButton } from "./print-button";

export const metadata = { title: "Invoice" };

export default async function InvoicePage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const session = await auth();

  const order = await db.order.findFirst({
    where: { id, userId: session!.user.id },
    include: {
      items: true,
      address: true,
      user: { select: { name: true, email: true } },
    },
  });

  if (!order) notFound();

  const gstRate = 5;
  const taxableValue = order.total - order.taxAmount;

  return (
    <>
      <style>{`
        @media print {
          .no-print { display: none !important; }
          body { background: white; color: black; }
          .invoice { max-width: 100%; padding: 0; }
        }
        body { font-family: system-ui, sans-serif; }
      `}</style>

      {/* Print button */}
      <div className="no-print mb-6 flex items-center justify-between">
        <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Invoice</h2>
        <PrintButton />
      </div>

      <div className="invoice mx-auto max-w-2xl rounded-xl border border-gray-200 bg-white p-8 text-gray-800 shadow-sm">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-gray-200 pb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Nayabi Collection</h1>
            <p className="mt-1 text-sm text-gray-500">nayabicollection.com</p>
          </div>
          <div className="text-right">
            <p className="text-lg font-semibold text-gray-900">TAX INVOICE</p>
            <p className="mt-1 text-sm text-gray-600">{order.orderNumber}</p>
            <p className="text-sm text-gray-500">
              {new Date(order.createdAt).toLocaleDateString("en-IN", {
                day: "numeric",
                month: "long",
                year: "numeric",
              })}
            </p>
          </div>
        </div>

        {/* Bill to */}
        {order.address && (
          <div className="mt-6 grid gap-6 sm:grid-cols-2">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">Bill To</p>
              <address className="mt-2 not-italic text-sm text-gray-700 leading-relaxed">
                <strong>{order.address.name}</strong>
                <br />
                {order.address.line1}
                {order.address.line2 && <>, {order.address.line2}</>}
                <br />
                {order.address.city}, {order.address.state} – {order.address.pincode}
                <br />
                +91 {order.address.phone}
              </address>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">Payment</p>
              <p className="mt-2 text-sm text-gray-700">{order.paymentMethod}</p>
              <p className="mt-1 text-xs font-semibold uppercase tracking-wide text-gray-400 mt-4">Status</p>
              <p className="mt-1 text-sm font-medium text-gray-700">{order.orderStatus}</p>
            </div>
          </div>
        )}

        {/* Items */}
        <table className="mt-8 w-full text-sm">
          <thead>
            <tr className="border-b border-gray-200">
              <th className="pb-2 text-left font-semibold text-gray-600">Item</th>
              <th className="pb-2 text-right font-semibold text-gray-600">Qty</th>
              <th className="pb-2 text-right font-semibold text-gray-600">Unit Price</th>
              <th className="pb-2 text-right font-semibold text-gray-600">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {order.items.map((item) => (
              <tr key={item.id}>
                <td className="py-3 pr-4">
                  <p className="font-medium text-gray-800">{item.productName}</p>
                  {item.variantDetails && (
                    <p className="text-xs text-gray-500">{item.variantDetails}</p>
                  )}
                </td>
                <td className="py-3 text-right text-gray-700">{item.quantity}</td>
                <td className="py-3 text-right text-gray-700">
                  {formatPrice(Math.round(item.totalPrice / item.quantity))}
                </td>
                <td className="py-3 text-right font-medium text-gray-800">
                  {formatPrice(item.totalPrice)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Totals */}
        <div className="mt-6 flex justify-end">
          <dl className="w-64 text-sm">
            <div className="flex justify-between py-1">
              <dt className="text-gray-500">Subtotal</dt>
              <dd className="text-gray-700">{formatPrice(order.subtotal)}</dd>
            </div>
            {order.discountAmount > 0 && (
              <div className="flex justify-between py-1">
                <dt className="text-gray-500">Discount</dt>
                <dd className="text-green-600">−{formatPrice(order.discountAmount)}</dd>
              </div>
            )}
            <div className="flex justify-between py-1">
              <dt className="text-gray-500">Shipping</dt>
              <dd className="text-gray-700">
                {order.shippingCost === 0 ? "Free" : formatPrice(order.shippingCost)}
              </dd>
            </div>
            {order.codCharge > 0 && (
              <div className="flex justify-between py-1">
                <dt className="text-gray-500">COD charge</dt>
                <dd className="text-gray-700">{formatPrice(order.codCharge)}</dd>
              </div>
            )}
            <div className="flex justify-between py-1">
              <dt className="text-gray-500">Taxable value</dt>
              <dd className="text-gray-700">{formatPrice(taxableValue)}</dd>
            </div>
            {order.taxAmount > 0 && (
              <div className="flex justify-between py-1">
                <dt className="text-gray-500">GST ({gstRate}%)</dt>
                <dd className="text-gray-700">{formatPrice(order.taxAmount)}</dd>
              </div>
            )}
            <div className="flex justify-between border-t border-gray-200 py-2 mt-1">
              <dt className="font-bold text-gray-900">Total</dt>
              <dd className="font-bold text-gray-900">{formatPrice(order.total)}</dd>
            </div>
          </dl>
        </div>

        {/* GST note */}
        <p className="mt-8 text-xs text-gray-400 border-t border-gray-100 pt-4">
          This is a computer-generated invoice and does not require a signature.
          GST is included in the total at {gstRate}%. For queries: support@nayabicollection.com
        </p>
      </div>
    </>
  );
}
