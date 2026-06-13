// Prisma 7 generated types — re-export for convenience
export type { User, Product, Order, Category, Collection } from "../generated/prisma/client";

/** Cart item in Zustand store (client-side) */
export interface CartStoreItem {
  variantId: string;
  productId: string;
  productName: string;
  variantLabel: string; // e.g. "Black · Size M · Chiffon"
  imageUrl: string;
  price: number; // paise
  quantity: number;
  maxStock: number;
}

/** Minimal product card data for listings */
export interface ProductCard {
  id: string;
  name: string;
  slug: string;
  price: number;
  comparePrice: number | null;
  images: string[];
  category: { name: string; slug: string };
  rating?: number;
  reviewCount?: number;
  isLowStock?: boolean;
  isOutOfStock?: boolean;
}

/** Admin sidebar nav item */
export interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: number;
  roles?: ("SUPER_ADMIN" | "MANAGER" | "FULFILLMENT")[];
}

/** API response wrapper */
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

/** Pagination */
export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}
