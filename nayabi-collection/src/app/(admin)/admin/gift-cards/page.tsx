import { Gift } from "lucide-react";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge } from "@/components/ui";
import { formatPrice } from "@/lib/utils";
import { IssueGiftCardForm } from "./issue-form";

export const metadata = { title: "Gift Cards | Admin" };
export const dynamic = "force-dynamic";

interface GiftCardRow {
  id: string;
  code: string;
  balance: number;
  initialValue: number;
  status: string;
  recipientEmail: string | null;
  createdAt: Date;
}

async function getGiftCards(): Promise<GiftCardRow[]> {
  try {
    return await db.giftCard.findMany({
      orderBy: { createdAt: "desc" },
      take: 100,
      select: {
        id: true,
        code: true,
        balance: true,
        initialValue: true,
        status: true,
        recipientEmail: true,
        createdAt: true,
      },
    });
  } catch {
    return [];
  }
}

const statusVariant: Record<string, "success" | "warning" | "error" | "neutral"> = {
  ACTIVE: "success",
  REDEEMED: "neutral",
  EXPIRED: "warning",
  DISABLED: "error",
};

export default async function AdminGiftCardsPage() {
  const cards = await getGiftCards();
  const totalOutstanding = cards
    .filter((c) => c.status === "ACTIVE")
    .reduce((s, c) => s + c.balance, 0);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="flex items-center gap-2 text-2xl font-semibold text-[var(--color-text-primary)]">
          <Gift className="h-6 w-6 text-[var(--color-gold)]" aria-hidden="true" />
          Gift Cards
        </h2>
        <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
          Issue and track store gift cards. Outstanding active balance:{" "}
          <strong className="text-[var(--color-text-primary)]">
            {formatPrice(totalOutstanding)}
          </strong>
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
        {/* Issue */}
        <GlassCard padding="md" className="h-fit">
          <h3 className="text-base font-semibold text-[var(--color-text-primary)]">
            Issue a gift card
          </h3>
          <p className="mt-1 mb-4 text-xs text-[var(--color-text-muted)]">
            Generates a unique code and records an ISSUE transaction.
          </p>
          <IssueGiftCardForm />
        </GlassCard>

        {/* List */}
        <GlassCard padding="md">
          <div className="overflow-x-auto">
            <table className="w-full text-sm" aria-label="Gift cards">
              <thead>
                <tr className="border-b border-[var(--color-glass-border)]">
                  {["Code", "Balance", "Initial", "Status", "Recipient", "Issued"].map(
                    (h) => (
                      <th
                        key={h}
                        scope="col"
                        className="pr-4 pb-2 text-left text-xs font-medium whitespace-nowrap text-[var(--color-text-muted)] last:pr-0"
                      >
                        {h}
                      </th>
                    )
                  )}
                </tr>
              </thead>
              <tbody>
                {cards.map((c) => (
                  <tr
                    key={c.id}
                    className="border-b border-[var(--color-glass-border)] last:border-0"
                  >
                    <td className="py-3 pr-4 font-mono text-xs text-[var(--color-text-primary)]">
                      {c.code}
                    </td>
                    <td className="py-3 pr-4 font-semibold whitespace-nowrap text-[var(--color-gold)]">
                      {formatPrice(c.balance)}
                    </td>
                    <td className="py-3 pr-4 whitespace-nowrap text-[var(--color-text-secondary)]">
                      {formatPrice(c.initialValue)}
                    </td>
                    <td className="py-3 pr-4">
                      <GlassBadge variant={statusVariant[c.status] ?? "neutral"}>
                        {c.status}
                      </GlassBadge>
                    </td>
                    <td className="max-w-[160px] truncate py-3 pr-4 text-[var(--color-text-secondary)]">
                      {c.recipientEmail ?? "—"}
                    </td>
                    <td className="py-3 text-xs whitespace-nowrap text-[var(--color-text-muted)]">
                      {new Date(c.createdAt).toLocaleDateString("en-IN")}
                    </td>
                  </tr>
                ))}
                {cards.length === 0 && (
                  <tr>
                    <td
                      colSpan={6}
                      className="py-12 text-center text-sm text-[var(--color-text-muted)]"
                    >
                      No gift cards issued yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </GlassCard>
      </div>
    </div>
  );
}
