"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { CartStoreItem } from "@/types";

interface CartState {
  items: CartStoreItem[];
  isOpen: boolean;
  open: () => void;
  close: () => void;
  addItem: (item: CartStoreItem) => void;
  removeItem: (variantId: string) => void;
  setQuantity: (variantId: string, quantity: number) => void;
  clear: () => void;
  subtotal: () => number;
  count: () => number;
}

export const useCart = create<CartState>()(
  persist(
    (set, get) => ({
      items: [],
      isOpen: false,
      open: () => set({ isOpen: true }),
      close: () => set({ isOpen: false }),

      addItem: (item) =>
        set((state) => {
          const existing = state.items.find((i) => i.variantId === item.variantId);
          if (existing) {
            return {
              items: state.items.map((i) =>
                i.variantId === item.variantId
                  ? { ...i, quantity: Math.min(i.quantity + item.quantity, i.maxStock) }
                  : i
              ),
              isOpen: true,
            };
          }
          return { items: [...state.items, item], isOpen: true };
        }),

      removeItem: (variantId) =>
        set((state) => ({ items: state.items.filter((i) => i.variantId !== variantId) })),

      setQuantity: (variantId, quantity) =>
        set((state) => ({
          items:
            quantity <= 0
              ? state.items.filter((i) => i.variantId !== variantId)
              : state.items.map((i) =>
                  i.variantId === variantId
                    ? { ...i, quantity: Math.min(quantity, i.maxStock) }
                    : i
                ),
        })),

      clear: () => set({ items: [] }),
      subtotal: () => get().items.reduce((s, i) => s + i.price * i.quantity, 0),
      count: () => get().items.reduce((s, i) => s + i.quantity, 0),
    }),
    {
      name: "nayabi-cart",
      partialize: (s) => ({ items: s.items }) as CartState,
    }
  )
);
