/**
 * Static demo product/category data shown when the database is not yet configured.
 * Swap in real data by seeding the DB (npx prisma db seed).
 */
import type { ProductCard } from "@/types";

const us = (id: string, w = 800) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${w}&q=85`;

export const DEMO_CATEGORIES = [
  { id: "cat-1", name: "Hijabs",       slug: "hijabs",       isActive: true, sortOrder: 1, parentId: null, image: us("photo-1611507929918-08e9e7da2dd4"), createdAt: new Date(), updatedAt: new Date() },
  { id: "cat-2", name: "Abayas",       slug: "abayas",       isActive: true, sortOrder: 2, parentId: null, image: us("photo-1631233859262-0d62ed426d7b"), createdAt: new Date(), updatedAt: new Date() },
  { id: "cat-3", name: "Namaz Scarfs", slug: "namaz-scarfs", isActive: true, sortOrder: 3, parentId: null, image: us("photo-1591100063942-9b1e89d2d0b1"), createdAt: new Date(), updatedAt: new Date() },
  { id: "cat-4", name: "Accessories",  slug: "accessories",  isActive: true, sortOrder: 4, parentId: null, image: us("photo-1576053139778-7e32f2ae3cfd"), createdAt: new Date(), updatedAt: new Date() },
];

export const DEMO_PRODUCTS: ProductCard[] = [
  /* ── HIJABS ── */
  {
    id: "p-1",
    name: "Silk Georgette Hijab",
    slug: "silk-georgette-hijab",
    price: 59900,
    comparePrice: 79900,
    images: [us("photo-1611507929918-08e9e7da2dd4")],
    category: { name: "Hijabs", slug: "hijabs" },
    rating: 4.8,
    reviewCount: 124,
  },
  {
    id: "p-2",
    name: "Chiffon Pashmina Hijab",
    slug: "chiffon-pashmina-hijab",
    price: 49900,
    comparePrice: 64900,
    images: [us("photo-1583391733956-6c78276477e2")],
    category: { name: "Hijabs", slug: "hijabs" },
    rating: 4.7,
    reviewCount: 88,
  },
  {
    id: "p-3",
    name: "Cotton Jersey Hijab",
    slug: "cotton-jersey-hijab",
    price: 39900,
    comparePrice: null,
    images: [us("photo-1561037404-61cd46aa615b")],
    category: { name: "Hijabs", slug: "hijabs" },
    rating: 4.9,
    reviewCount: 202,
  },
  {
    id: "p-4",
    name: "Embroidered Nida Hijab",
    slug: "embroidered-nida-hijab",
    price: 74900,
    comparePrice: 99900,
    images: [us("photo-1520012218364-3dbe62c99bee")],
    category: { name: "Hijabs", slug: "hijabs" },
    rating: 4.6,
    reviewCount: 57,
    isLowStock: true,
  },
  /* ── ABAYAS ── */
  {
    id: "p-5",
    name: "Classic Black Abaya",
    slug: "classic-black-abaya",
    price: 189900,
    comparePrice: 249900,
    images: [us("photo-1631233859262-0d62ed426d7b")],
    category: { name: "Abayas", slug: "abayas" },
    rating: 4.9,
    reviewCount: 310,
  },
  {
    id: "p-6",
    name: "Open-Front Farasha Abaya",
    slug: "open-front-farasha-abaya",
    price: 229900,
    comparePrice: 299900,
    images: [us("photo-1583391733956-6c78276477e2")],
    category: { name: "Abayas", slug: "abayas" },
    rating: 4.7,
    reviewCount: 145,
  },
  {
    id: "p-7",
    name: "Crepe Butterfly Abaya",
    slug: "crepe-butterfly-abaya",
    price: 169900,
    comparePrice: null,
    images: [us("photo-1520012218364-3dbe62c99bee")],
    category: { name: "Abayas", slug: "abayas" },
    rating: 4.8,
    reviewCount: 93,
  },
  {
    id: "p-8",
    name: "Embellished Occasion Abaya",
    slug: "embellished-occasion-abaya",
    price: 349900,
    comparePrice: 449900,
    images: [us("photo-1631233859262-0d62ed426d7b")],
    category: { name: "Abayas", slug: "abayas" },
    rating: 5.0,
    reviewCount: 42,
    isLowStock: true,
  },
  /* ── NAMAZ SCARFS ── */
  {
    id: "p-9",
    name: "Premium Prayer Scarf Set",
    slug: "premium-prayer-scarf-set",
    price: 89900,
    comparePrice: 119900,
    images: [us("photo-1591100063942-9b1e89d2d0b1")],
    category: { name: "Namaz Scarfs", slug: "namaz-scarfs" },
    rating: 4.9,
    reviewCount: 267,
  },
  {
    id: "p-10",
    name: "Travel-Fold Prayer Scarf",
    slug: "travel-fold-prayer-scarf",
    price: 69900,
    comparePrice: null,
    images: [us("photo-1583391733956-6c78276477e2")],
    category: { name: "Namaz Scarfs", slug: "namaz-scarfs" },
    rating: 4.7,
    reviewCount: 134,
  },
  {
    id: "p-11",
    name: "Embroidered Ramadan Scarf",
    slug: "embroidered-ramadan-scarf",
    price: 129900,
    comparePrice: 149900,
    images: [us("photo-1611507929918-08e9e7da2dd4")],
    category: { name: "Namaz Scarfs", slug: "namaz-scarfs" },
    rating: 4.8,
    reviewCount: 78,
    isLowStock: true,
  },
  /* ── ACCESSORIES ── */
  {
    id: "p-12",
    name: "Satin Hijab Pin Set (6 pcs)",
    slug: "satin-hijab-pin-set",
    price: 24900,
    comparePrice: 34900,
    images: [us("photo-1576053139778-7e32f2ae3cfd")],
    category: { name: "Accessories", slug: "accessories" },
    rating: 4.6,
    reviewCount: 412,
  },
];

export const DEMO_BESTSELLERS = DEMO_PRODUCTS.filter((p) =>
  ["p-1", "p-5", "p-9", "p-12"].includes(p.id)
);

export const DEMO_NEW_ARRIVALS = DEMO_PRODUCTS.filter((p) =>
  ["p-4", "p-7", "p-8", "p-11"].includes(p.id)
);
