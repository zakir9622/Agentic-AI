/* Seed demo catalogue: categories + products + variants.
   Run with: npm run db:seed (requires DATABASE_URL) */
import { PrismaClient } from "../src/generated/prisma/client";
import { PrismaPg } from "@prisma/adapter-pg";

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL! });
const db = new PrismaClient({ adapter } as ConstructorParameters<typeof PrismaClient>[0]);

const img = (id: string) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=800&q=80`;

async function main() {
  const categories = await Promise.all(
    [
      { name: "Hijabs", slug: "hijabs", sortOrder: 1, image: img("photo-1611507929918-08e9e7da2dd4") },
      { name: "Abayas", slug: "abayas", sortOrder: 2, image: img("photo-1631233859262-0d62ed426d7b") },
      { name: "Namaz Scarfs", slug: "namaz-scarfs", sortOrder: 3, image: img("photo-1591100063942-9b1e89d2d0b1") },
      { name: "Accessories", slug: "accessories", sortOrder: 4, image: img("photo-1576053139778-7e32f2ae3cfd") },
    ].map((c) =>
      db.category.upsert({ where: { slug: c.slug }, create: c, update: c })
    )
  );
  const bySlug = Object.fromEntries(categories.map((c) => [c.slug, c.id]));

  const products: Array<{
    name: string;
    slug: string;
    description: string;
    categoryId: string;
    price: number;
    comparePrice?: number;
    images: string[];
    tags: string[];
    variants: Array<{ color?: string; size?: string; fabric?: string; sku: string; stock: number }>;
  }> = [
    {
      name: "Classic Chiffon Hijab",
      slug: "classic-chiffon-hijab",
      description:
        "Lightweight premium chiffon hijab with a soft matte finish. Breathable, opaque and easy to style.\n\nCare: Hand wash cold, hang dry.",
      categoryId: bySlug["hijabs"],
      price: 59900,
      comparePrice: 79900,
      images: [img("photo-1611507929918-08e9e7da2dd4")],
      tags: ["hijab", "chiffon", "everyday"],
      variants: [
        { color: "Black", fabric: "Chiffon", sku: "HIJ-CHF-BLK", stock: 25 },
        { color: "Dusty Rose", fabric: "Chiffon", sku: "HIJ-CHF-ROS", stock: 18 },
        { color: "Emerald", fabric: "Chiffon", sku: "HIJ-CHF-EMR", stock: 12 },
        { color: "Navy", fabric: "Chiffon", sku: "HIJ-CHF-NVY", stock: 0 },
      ],
    },
    {
      name: "Premium Jersey Hijab",
      slug: "premium-jersey-hijab",
      description:
        "Stretchy, pin-free jersey hijab that stays in place all day. Perfect for active lifestyles.\n\nCare: Machine wash gentle, lay flat to dry.",
      categoryId: bySlug["hijabs"],
      price: 69900,
      images: [img("photo-1622445275576-721325763afe")],
      tags: ["hijab", "jersey", "sport"],
      variants: [
        { color: "Black", fabric: "Jersey", sku: "HIJ-JRS-BLK", stock: 30 },
        { color: "Mocha", fabric: "Jersey", sku: "HIJ-JRS-MOC", stock: 22 },
        { color: "Grey", fabric: "Jersey", sku: "HIJ-JRS-GRY", stock: 8 },
      ],
    },
    {
      name: "Embroidered Open Abaya",
      slug: "embroidered-open-abaya",
      description:
        "Elegant open-front abaya with delicate gold-thread embroidery on the cuffs. Flowing nida fabric with a luxurious drape.\n\nCare: Dry clean recommended.",
      categoryId: bySlug["abayas"],
      price: 249900,
      comparePrice: 299900,
      images: [img("photo-1631233859262-0d62ed426d7b")],
      tags: ["abaya", "embroidered", "occasion"],
      variants: [
        { color: "Black", size: "52", fabric: "Nida", sku: "ABA-EMB-BLK-52", stock: 10 },
        { color: "Black", size: "54", fabric: "Nida", sku: "ABA-EMB-BLK-54", stock: 14 },
        { color: "Black", size: "56", fabric: "Nida", sku: "ABA-EMB-BLK-56", stock: 9 },
        { color: "Maroon", size: "54", fabric: "Nida", sku: "ABA-EMB-MAR-54", stock: 5 },
      ],
    },
    {
      name: "Everyday Closed Abaya",
      slug: "everyday-closed-abaya",
      description:
        "Minimalist closed abaya in soft crepe — a wardrobe essential. Front zip, side pockets, relaxed fit.\n\nCare: Machine wash cold.",
      categoryId: bySlug["abayas"],
      price: 179900,
      images: [img("photo-1594632913202-2810b8b9b2b5")],
      tags: ["abaya", "everyday", "crepe"],
      variants: [
        { color: "Black", size: "52", fabric: "Crepe", sku: "ABA-EVD-BLK-52", stock: 20 },
        { color: "Black", size: "54", fabric: "Crepe", sku: "ABA-EVD-BLK-54", stock: 25 },
        { color: "Charcoal", size: "54", fabric: "Crepe", sku: "ABA-EVD-CHR-54", stock: 15 },
      ],
    },
    {
      name: "Prayer Scarf Set",
      slug: "prayer-scarf-set",
      description:
        "Two-piece namaz scarf set with attached bonnet. Full coverage, soft breathable cotton blend — ideal for daily prayers and travel.\n\nCare: Machine wash gentle.",
      categoryId: bySlug["namaz-scarfs"],
      price: 89900,
      comparePrice: 109900,
      images: [img("photo-1591100063942-9b1e89d2d0b1")],
      tags: ["namaz", "prayer", "set"],
      variants: [
        { color: "White", size: "Free Size", fabric: "Cotton", sku: "NMZ-SET-WHT", stock: 40 },
        { color: "Black", size: "Free Size", fabric: "Cotton", sku: "NMZ-SET-BLK", stock: 35 },
        { color: "Lavender", size: "Free Size", fabric: "Cotton", sku: "NMZ-SET-LAV", stock: 3 },
      ],
    },
    {
      name: "Magnetic Hijab Pins (Set of 6)",
      slug: "magnetic-hijab-pins",
      description:
        "Strong no-snag magnetic pins that hold layers securely without damaging delicate fabrics. Matte finish in assorted colours.",
      categoryId: bySlug["accessories"],
      price: 39900,
      images: [img("photo-1576053139778-7e32f2ae3cfd")],
      tags: ["accessories", "pins"],
      variants: [{ color: "Assorted", sku: "ACC-PIN-AST", stock: 60 }],
    },
  ];

  for (const p of products) {
    const { variants, ...data } = p;
    const product = await db.product.upsert({
      where: { slug: p.slug },
      create: data,
      update: data,
    });
    for (const v of variants) {
      await db.productVariant.upsert({
        where: { sku: v.sku },
        create: { ...v, productId: product.id },
        update: { ...v, productId: product.id },
      });
    }
  }

  await db.announcementBar.upsert({
    where: { id: "seed-banner" },
    create: {
      id: "seed-banner",
      text: "Free shipping on orders above ₹999",
      isActive: true,
    },
    update: {},
  });

  console.log("✓ Seeded categories, products, variants, announcement bar");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(() => db.$disconnect());
