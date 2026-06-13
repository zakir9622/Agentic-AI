"use client";

import * as React from "react";
import Link from "next/link";
import { GlassCard, GlassButton, GlassBadge } from "@/components/ui";
import { Star, Check, X } from "lucide-react";

interface Review {
  id: string;
  rating: number;
  title: string | null;
  body: string | null;
  isApproved: boolean;
  isVerifiedPurchase: boolean;
  createdAt: string;
  product: { name: string; slug: string };
  user: { name: string | null; email: string };
}

export default function AdminReviewsPage() {
  const [reviews, setReviews] = React.useState<Review[]>([]);
  const [filter, setFilter] = React.useState<"false" | "true" | "">( "false");
  const [loading, setLoading] = React.useState(true);
  const [busy, setBusy] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    fetch(`/api/admin/reviews?approved=${filter}`)
      .then((r) => r.json())
      .then((data) => {
        if (active) {
          setReviews(data);
          setLoading(false);
        }
      })
      .catch(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [filter]);

  async function handleApprove(reviewId: string, approve: boolean) {
    setBusy(reviewId);
    await fetch("/api/admin/reviews", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reviewId, approve }),
    });
    setReviews((prev) => prev.filter((r) => r.id !== reviewId));
    setBusy(null);
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Reviews</h2>
        <div className="flex gap-2">
          {(["false", "true", ""] as const).map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`rounded-lg px-3 py-1 text-sm transition-colors ${
                filter === f
                  ? "bg-[var(--color-gold)] text-[var(--color-bg-base)]"
                  : "text-[var(--color-text-secondary)] hover:text-[var(--color-gold)]"
              }`}
            >
              {f === "false" ? "Pending" : f === "true" ? "Approved" : "All"}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <p className="text-sm text-[var(--color-text-muted)]">Loading…</p>
      ) : reviews.length === 0 ? (
        <GlassCard padding="md" className="text-center">
          <p className="text-sm text-[var(--color-text-muted)]">No reviews in this queue.</p>
        </GlassCard>
      ) : (
        <ul className="flex flex-col gap-4" role="list">
          {reviews.map((r) => (
            <li key={r.id}>
              <GlassCard padding="md">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <Link
                        href={`/products/${r.product.slug}`}
                        className="text-sm font-medium text-[var(--color-text-primary)] hover:text-[var(--color-gold)]"
                        target="_blank"
                      >
                        {r.product.name}
                      </Link>
                      {r.isVerifiedPurchase && (
                        <GlassBadge variant="success">Verified purchase</GlassBadge>
                      )}
                      {r.isApproved && (
                        <GlassBadge variant="info">Approved</GlassBadge>
                      )}
                    </div>
                    <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">
                      {r.user.name ?? r.user.email} ·{" "}
                      {new Date(r.createdAt).toLocaleDateString("en-IN")}
                    </p>
                    <div className="mt-2 flex gap-0.5">
                      {Array.from({ length: 5 }, (_, i) => (
                        <Star
                          key={i}
                          aria-hidden="true"
                          className={
                            i < r.rating
                              ? "h-3.5 w-3.5 fill-[var(--color-gold)] text-[var(--color-gold)]"
                              : "h-3.5 w-3.5 text-[var(--color-text-muted)]"
                          }
                        />
                      ))}
                    </div>
                    {r.title && (
                      <p className="mt-2 text-sm font-medium text-[var(--color-text-primary)]">{r.title}</p>
                    )}
                    {r.body && (
                      <p className="mt-1 text-sm text-[var(--color-text-secondary)] line-clamp-3">{r.body}</p>
                    )}
                  </div>

                  <div className="flex gap-2 shrink-0">
                    {!r.isApproved && (
                      <GlassButton
                        size="sm"
                        variant="secondary"
                        disabled={busy === r.id}
                        onClick={() => handleApprove(r.id, true)}
                      >
                        <Check className="h-3.5 w-3.5 mr-1" aria-hidden="true" />
                        Approve
                      </GlassButton>
                    )}
                    <GlassButton
                      size="sm"
                      variant="ghost"
                      disabled={busy === r.id}
                      onClick={() => handleApprove(r.id, false)}
                      className="text-[var(--color-error)]"
                    >
                      <X className="h-3.5 w-3.5 mr-1" aria-hidden="true" />
                      {r.isApproved ? "Unapprove" : "Reject"}
                    </GlassButton>
                  </div>
                </div>
              </GlassCard>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
