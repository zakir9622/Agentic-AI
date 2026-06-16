"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { ProductCard } from "@/types";

interface RecentlyViewedState {
  items: ProductCard[];
  add: (product: ProductCard) => void;
  clear: () => void;
}

const MAX_ITEMS = 8;

/**
 * Client-side "recently viewed" history, persisted to localStorage. Most recent
 * first, de-duplicated by product id, capped at MAX_ITEMS.
 */
export const useRecentlyViewed = create<RecentlyViewedState>()(
  persist(
    (set) => ({
      items: [],
      add: (product) =>
        set((state) => ({
          items: [product, ...state.items.filter((p) => p.id !== product.id)].slice(
            0,
            MAX_ITEMS
          ),
        })),
      clear: () => set({ items: [] }),
    }),
    { name: "nayabi-recently-viewed" }
  )
);
