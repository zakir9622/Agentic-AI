"use client";

import * as React from "react";
import { Star } from "lucide-react";
import { GlassCard, GlassButton } from "@/components/ui";
import { submitReviewAction, type ActionState } from "@/app/actions/account";

export function ReviewForm({ productId }: { productId: string }) {
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    submitReviewAction,
    {}
  );
  const [rating, setRating] = React.useState(0);
  const [hovered, setHovered] = React.useState(0);

  if (state.ok) {
    return (
      <GlassCard padding="md" className="text-center">
        <p className="text-sm text-[var(--color-success)]">✓ {state.message}</p>
      </GlassCard>
    );
  }

  return (
    <GlassCard padding="md">
      <h3 className="text-sm font-semibold text-[var(--color-text-primary)] mb-4">
        Write a review
      </h3>
      <form action={formAction} className="flex flex-col gap-4">
        <input type="hidden" name="productId" value={productId} />
        <input type="hidden" name="rating" value={rating} />

        {/* Star picker */}
        <div>
          <p className="mb-1.5 text-sm text-[var(--color-text-secondary)]">Your rating</p>
          <div className="flex gap-1" role="group" aria-label="Select star rating">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                type="button"
                aria-label={`${star} star${star > 1 ? "s" : ""}`}
                onClick={() => setRating(star)}
                onMouseEnter={() => setHovered(star)}
                onMouseLeave={() => setHovered(0)}
              >
                <Star
                  className={`h-6 w-6 transition-colors ${
                    star <= (hovered || rating)
                      ? "fill-[var(--color-gold)] text-[var(--color-gold)]"
                      : "text-[var(--color-text-muted)]"
                  }`}
                  aria-hidden="true"
                />
              </button>
            ))}
          </div>
          {state.errors?.rating && (
            <p className="mt-1 text-xs text-[var(--color-error)]">{state.errors.rating}</p>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="review-title" className="text-sm text-[var(--color-text-secondary)]">
            Title (optional)
          </label>
          <input
            id="review-title"
            name="title"
            type="text"
            maxLength={100}
            placeholder="Summarise your experience"
            className="glass-input"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="review-body" className="text-sm text-[var(--color-text-secondary)]">
            Review
          </label>
          <textarea
            id="review-body"
            name="body"
            rows={4}
            maxLength={2000}
            placeholder="What did you love (or not)? How was the quality, fit, fabric?"
            className="glass-input resize-none"
          />
        </div>

        {state.message && !state.ok && (
          <p role="alert" className="text-sm text-[var(--color-error)]">{state.message}</p>
        )}

        <GlassButton
          type="submit"
          size="sm"
          loading={pending}
          disabled={rating === 0}
        >
          Submit review
        </GlassButton>
      </form>
    </GlassCard>
  );
}
