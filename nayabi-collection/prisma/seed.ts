/* Seed: categories + realistic Nayabi Collection product catalogue.
   Run: npx prisma db seed  (requires DATABASE_URL) */

import "dotenv/config";
import { PrismaClient } from "../src/generated/prisma/client";
import { PrismaPg } from "@prisma/adapter-pg";
import bcrypt from "bcryptjs";

const adapter = new PrismaPg({ connectionString: process.env.DATABASE_URL! });
const db = new PrismaClient({ adapter } as ConstructorParameters<typeof PrismaClient>[0]);

/* Unsplash helpers */
const us = (id: string, w = 800) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${w}&q=85`;

async function main() {
  /* ── ADMIN USER ─────────────────────────────────────────────────────────── */
  const adminPassword = await bcrypt.hash("Admin@1234!", 12);
  await db.adminUser.upsert({
    where: { email: "admin@nayabicollection.com" },
    create: {
      name: "Admin",
      email: "admin@nayabicollection.com",
      hashedPassword: adminPassword,
      role: "SUPER_ADMIN",
    },
    update: {},
  });

  /* ── CATEGORIES ─────────────────────────────────────────────────────────── */
  const catDefs = [
    {
      name: "Hijabs",
      slug: "hijabs",
      sortOrder: 1,
      image: us("photo-1611507929918-08e9e7da2dd4"),
    },
    {
      name: "Abayas",
      slug: "abayas",
      sortOrder: 2,
      image: us("photo-1631233859262-0d62ed426d7b"),
    },
    {
      name: "Namaz Scarfs",
      slug: "namaz-scarfs",
      sortOrder: 3,
      image: us("photo-1591100063942-9b1e89d2d0b1"),
    },
    {
      name: "Accessories",
      slug: "accessories",
      sortOrder: 4,
      image: us("photo-1576053139778-7e32f2ae3cfd"),
    },
  ];

  const categories = await Promise.all(
    catDefs.map((c) =>
      db.category.upsert({ where: { slug: c.slug }, create: c, update: c })
    )
  );
  const bySlug = Object.fromEntries(categories.map((c) => [c.slug, c.id]));

  /* ── PRODUCTS ───────────────────────────────────────────────────────────── */
  type Variant = {
    color?: string;
    size?: string;
    fabric?: string;
    sku: string;
    stock: number;
  };

  const products: Array<{
    name: string;
    slug: string;
    description: string;
    categoryId: string;
    price: number;
    comparePrice?: number;
    images: string[];
    tags: string[];
    seoTitle?: string;
    seoDescription?: string;
    variants: Variant[];
  }> = [
    /* ── HIJABS ── */
    {
      name: "Silk Georgette Hijab",
      slug: "silk-georgette-hijab",
      description:
        "Crafted from luxurious silk-blend georgette, this hijab offers a natural sheen and an elegant drape that elevates any look. The lightweight fabric feels cool against the skin — perfect for Indian summers and air-conditioned offices alike.\n\nSubtle texture prevents slipping, making it suitable for all-day wear without constant adjustments. Modest opacity with no underlining needed.\n\nCare: Hand-wash in cold water, lay flat to dry. Do not tumble-dry.",
      categoryId: bySlug["hijabs"],
      price: 59900,
      comparePrice: 79900,
      images: [us("photo-1611507929918-08e9e7da2dd4")],
      tags: ["hijab", "georgette", "silk", "premium", "everyday"],
      seoTitle: "Silk Georgette Hijab | Nayabi Collection",
      seoDescription:
        "Premium silk-blend georgette hijab with elegant drape. Lightweight, breathable, and fully opaque. Free shipping above ₹999.",
      variants: [
        { color: "Black",     fabric: "Georgette", sku: "HIJ-GEO-001-BLK", stock: 28 },
        { color: "Wine Red",  fabric: "Georgette", sku: "HIJ-GEO-001-WIN", stock: 18 },
        { color: "Emerald",   fabric: "Georgette", sku: "HIJ-GEO-001-EMR", stock: 15 },
        { color: "Cream",     fabric: "Georgette", sku: "HIJ-GEO-001-CRM", stock: 12 },
        { color: "Dusty Rose",fabric: "Georgette", sku: "HIJ-GEO-001-ROS", stock: 10 },
      ],
    },
    {
      name: "Everyday Jersey Hijab",
      slug: "everyday-jersey-hijab",
      description:
        "Our best-selling jersey hijab that stays in place all day without a single pin. The stretchy, breathable fabric is soft against the skin and moves with you — ideal for work, school, travel, and prayer.\n\nMachine-washable and quick-drying, this is the hijab you'll reach for every morning without thinking twice.\n\nCare: Machine wash gentle cycle, cold. Hang or lay flat to dry.",
      categoryId: bySlug["hijabs"],
      price: 34900,
      images: [us("photo-1622445275576-721325763afe")],
      tags: ["hijab", "jersey", "everyday", "pin-free", "washable"],
      variants: [
        { color: "Black",   fabric: "Jersey", sku: "HIJ-JRS-002-BLK", stock: 45 },
        { color: "Navy",    fabric: "Jersey", sku: "HIJ-JRS-002-NVY", stock: 38 },
        { color: "Charcoal",fabric: "Jersey", sku: "HIJ-JRS-002-CHR", stock: 30 },
        { color: "Mocha",   fabric: "Jersey", sku: "HIJ-JRS-002-MOC", stock: 25 },
        { color: "Grey",    fabric: "Jersey", sku: "HIJ-JRS-002-GRY", stock: 3  },
      ],
    },
    {
      name: "Premium Chiffon Hijab",
      slug: "premium-chiffon-hijab",
      description:
        "A wardrobe staple in ultra-lightweight chiffon with the perfect balance of opacity and breathability. The soft matte finish drapes beautifully for everyday styling and formal occasions alike.\n\nPairs effortlessly with abayas, kurtas, and modest dresses. Available in versatile neutrals and trending seasonal colours.\n\nCare: Hand-wash cold, hang dry. Iron on low heat if needed.",
      categoryId: bySlug["hijabs"],
      price: 39900,
      comparePrice: 54900,
      images: [us("photo-1609246123971-9c5d07de55f9", 800)],
      tags: ["hijab", "chiffon", "lightweight", "matte", "summer"],
      variants: [
        { color: "Black",     fabric: "Chiffon", sku: "HIJ-CHF-003-BLK", stock: 35 },
        { color: "Sage Green",fabric: "Chiffon", sku: "HIJ-CHF-003-SGE", stock: 22 },
        { color: "Peach",     fabric: "Chiffon", sku: "HIJ-CHF-003-PCH", stock: 18 },
        { color: "Lavender",  fabric: "Chiffon", sku: "HIJ-CHF-003-LAV", stock: 8  },
      ],
    },
    {
      name: "Ready-to-Wear Instant Hijab",
      slug: "ready-to-wear-instant-hijab",
      description:
        "The smartest addition to a busy woman's wardrobe — a pre-stitched hijab with a built-in bonnet that goes on in seconds. No pins, no fuss, no rearranging throughout the day.\n\nThe soft lycra blend moves with you and stays put from Fajr to Isha. Loved by students, working professionals, and new hijabis.\n\nCare: Machine wash gentle, cold. Reshape and hang to dry.",
      categoryId: bySlug["hijabs"],
      price: 44900,
      images: [us("photo-1605289982774-9a6fef564df8", 800)],
      tags: ["hijab", "instant", "ready-to-wear", "pin-free", "beginners"],
      variants: [
        { color: "Black", fabric: "Lycra Blend", sku: "HIJ-RDY-004-BLK", stock: 50 },
        { color: "Taupe", fabric: "Lycra Blend", sku: "HIJ-RDY-004-TAU", stock: 36 },
        { color: "Navy",  fabric: "Lycra Blend", sku: "HIJ-RDY-004-NVY", stock: 28 },
      ],
    },
    {
      name: "Formal Crepe Hijab",
      slug: "formal-crepe-hijab",
      description:
        "Structured, refined, and polished — this crepe hijab is the one for board meetings, Eid prayers, and every important moment in between. The crisp fabric holds its shape all day with zero effort.\n\nFull opacity with a beautiful matte finish. Drapes crisply and frames the face elegantly.\n\nCare: Hand-wash or dry clean for best results. Iron on low.",
      categoryId: bySlug["hijabs"],
      price: 54900,
      comparePrice: 69900,
      images: [us("photo-1617138058564-24e68d0d1df3", 800)],
      tags: ["hijab", "crepe", "formal", "office", "eid"],
      variants: [
        { color: "Black",    fabric: "Crepe", sku: "HIJ-CRP-005-BLK", stock: 30 },
        { color: "Dusty Rose",fabric: "Crepe", sku: "HIJ-CRP-005-ROS", stock: 20 },
        { color: "Navy Blue", fabric: "Crepe", sku: "HIJ-CRP-005-NVY", stock: 16 },
      ],
    },

    /* ── ABAYAS ── */
    {
      name: "Classic Open Abaya",
      slug: "classic-open-abaya",
      description:
        "The abaya that works with everything — a timeless open-front silhouette in premium polyester crepe that drapes with quiet elegance. Lightweight enough for year-round wear, structured enough for any occasion.\n\nWide flutter sleeves, relaxed fit, and a barely-there weight that makes it feel like wearing air. Available in three lengths.\n\nCare: Machine wash cold on gentle cycle. Hang immediately to avoid creasing.",
      categoryId: bySlug["abayas"],
      price: 129900,
      images: [us("photo-1631233859262-0d62ed426d7b")],
      tags: ["abaya", "open", "everyday", "crepe", "classic"],
      seoTitle: "Classic Open Abaya | Nayabi Collection",
      seoDescription:
        "Timeless open-front abaya in premium crepe fabric. Lightweight, elegant, and perfect for everyday wear. Sizes 52–58.",
      variants: [
        { color: "Black", size: "52", fabric: "Crepe", sku: "ABA-OPN-001-BLK-52", stock: 24 },
        { color: "Black", size: "54", fabric: "Crepe", sku: "ABA-OPN-001-BLK-54", stock: 30 },
        { color: "Black", size: "56", fabric: "Crepe", sku: "ABA-OPN-001-BLK-56", stock: 18 },
        { color: "Black", size: "58", fabric: "Crepe", sku: "ABA-OPN-001-BLK-58", stock: 10 },
      ],
    },
    {
      name: "Embroidered Kimono Abaya",
      slug: "embroidered-kimono-abaya",
      description:
        "Turn heads at weddings, Eid gatherings, and evening events with this stunning kimono-cut abaya featuring pearl and gold-thread embroidery along the front panels and cuffs.\n\nCrafted from premium nida fabric — silky, non-clingy, and beautifully opaque. Wide sleeves add a regal, flowing quality that photographs magnificently.\n\nCare: Dry clean recommended. Store in the garment bag provided.",
      categoryId: bySlug["abayas"],
      price: 249900,
      comparePrice: 319900,
      images: [us("photo-1594632913202-2810b8b9b2b5", 800)],
      tags: ["abaya", "kimono", "embroidered", "occasion", "luxury", "wedding", "eid"],
      variants: [
        { color: "Black",    size: "52", fabric: "Nida", sku: "ABA-KMN-002-BLK-52", stock: 8 },
        { color: "Black",    size: "54", fabric: "Nida", sku: "ABA-KMN-002-BLK-54", stock: 10 },
        { color: "Black",    size: "56", fabric: "Nida", sku: "ABA-KMN-002-BLK-56", stock: 6 },
        { color: "Charcoal", size: "54", fabric: "Nida", sku: "ABA-KMN-002-CHR-54", stock: 5 },
        { color: "Maroon",   size: "54", fabric: "Nida", sku: "ABA-KMN-002-MAR-54", stock: 4 },
      ],
    },
    {
      name: "Front-Zip Closed Abaya",
      slug: "front-zip-closed-abaya",
      description:
        "Clean lines, hidden front-zip, and two deep side pockets — this abaya means business. The streamlined silhouette in premium nida fabric is equally suited to office mornings, hospital visits, and Friday prayers.\n\nCut slightly slim through the body with a relaxed drape at the hem. The zip runs edge-to-edge with a hidden placket so no hardware shows.\n\nCare: Machine wash cold, gentle cycle. Remove from washer immediately and hang.",
      categoryId: bySlug["abayas"],
      price: 159900,
      images: [us("photo-1573497019940-1c28c88b4f3e", 800)],
      tags: ["abaya", "closed", "zip", "formal", "professional", "pockets"],
      variants: [
        { color: "Black", size: "52", fabric: "Nida", sku: "ABA-ZIP-003-BLK-52", stock: 22 },
        { color: "Black", size: "54", fabric: "Nida", sku: "ABA-ZIP-003-BLK-54", stock: 28 },
        { color: "Black", size: "56", fabric: "Nida", sku: "ABA-ZIP-003-BLK-56", stock: 18 },
        { color: "Navy",  size: "54", fabric: "Nida", sku: "ABA-ZIP-003-NVY-54", stock: 12 },
      ],
    },
    {
      name: "Pearl-Embroidered Occasion Abaya",
      slug: "pearl-embroidered-occasion-abaya",
      description:
        "Reserve this one for the moments that matter. Intricate pearl and bead embroidery cascades across the neckline, front placket, and cuffs of this open-front abaya — every movement catches the light.\n\nMade from the finest imported nida crepe with a luxuriously weighted drape. Lined at the top for modesty and structure. Comes with a matching organza storage bag.\n\nCare: Dry clean only. Handle embroidery gently.",
      categoryId: bySlug["abayas"],
      price: 349900,
      comparePrice: 449900,
      images: [us("photo-1524504388940-b1c1722653e1", 800)],
      tags: ["abaya", "pearl", "embroidered", "luxury", "celebration", "eid", "bridal"],
      variants: [
        { color: "Black", size: "52", fabric: "Nida", sku: "ABA-PRL-004-BLK-52", stock: 5  },
        { color: "Black", size: "54", fabric: "Nida", sku: "ABA-PRL-004-BLK-54", stock: 7  },
        { color: "Black", size: "56", fabric: "Nida", sku: "ABA-PRL-004-BLK-56", stock: 4  },
        { color: "Maroon",size: "54", fabric: "Nida", sku: "ABA-PRL-004-MAR-54", stock: 3  },
      ],
    },
    {
      name: "Casual Everyday Abaya",
      slug: "casual-everyday-abaya",
      description:
        "For days when comfort is everything. This relaxed open-front abaya in soft crinkled crepe is effortless to wear, machine-washable, and forgiving of a rushed morning.\n\nSlightly oversized fit with easy three-quarter sleeves and a self-tie belt that's completely optional. The crinkle texture hides minor creases beautifully.\n\nCare: Machine wash cold. Lay flat or hang to dry. No ironing needed.",
      categoryId: bySlug["abayas"],
      price: 99900,
      comparePrice: 129900,
      images: [us("photo-1590003025925-c71bef1e5d84", 800)],
      tags: ["abaya", "casual", "everyday", "crinkle", "relaxed", "comfy"],
      variants: [
        { color: "Black",    size: "Free Size", fabric: "Crinkle Crepe", sku: "ABA-CAS-005-BLK-FS", stock: 35 },
        { color: "Charcoal", size: "Free Size", fabric: "Crinkle Crepe", sku: "ABA-CAS-005-CHR-FS", stock: 28 },
        { color: "Navy",     size: "Free Size", fabric: "Crinkle Crepe", sku: "ABA-CAS-005-NVY-FS", stock: 22 },
      ],
    },

    /* ── NAMAZ SCARFS ── */
    {
      name: "Premium Cotton Prayer Set",
      slug: "premium-cotton-prayer-set",
      description:
        "A two-piece prayer set that's been a family favourite across generations. Made from 100% pure breathable cotton, it offers complete coverage with a lightweight feel that stays comfortable through long prayer sessions.\n\nIncludes the scarf and an attached bonnet with an adjustable inner band. Washes beautifully and retains its crisp white with every launder.\n\nCare: Machine wash warm, tumble dry low. Iron on medium to restore crispness.",
      categoryId: bySlug["namaz-scarfs"],
      price: 69900,
      comparePrice: 89900,
      images: [us("photo-1591100063942-9b1e89d2d0b1")],
      tags: ["namaz", "prayer", "cotton", "set", "breathable"],
      variants: [
        { color: "White",       size: "Free Size", fabric: "Cotton", sku: "NMZ-CTN-001-WHT", stock: 50 },
        { color: "Black",       size: "Free Size", fabric: "Cotton", sku: "NMZ-CTN-001-BLK", stock: 45 },
        { color: "Mint Green",  size: "Free Size", fabric: "Cotton", sku: "NMZ-CTN-001-MGR", stock: 30 },
        { color: "Light Pink",  size: "Free Size", fabric: "Cotton", sku: "NMZ-CTN-001-PNK", stock: 25 },
      ],
    },
    {
      name: "Travel Prayer Set",
      slug: "travel-prayer-set",
      description:
        "Designed for the travelling Muslim woman — this compact prayer set folds into a small pouch no bigger than your purse. The wrinkle-resistant microfiber pops back into shape the moment you unfold it.\n\nAir-dries in under 30 minutes, making it perfect for daily wash while travelling. Comes with a drawstring travel pouch.\n\nCare: Hand-wash or machine wash cold. Drip dry.",
      categoryId: bySlug["namaz-scarfs"],
      price: 49900,
      images: [us("photo-1604952564408-0df3e773f1d2", 800)],
      tags: ["namaz", "prayer", "travel", "portable", "microfiber", "compact"],
      variants: [
        { color: "White", size: "Free Size", fabric: "Microfiber", sku: "NMZ-TRV-002-WHT", stock: 40 },
        { color: "Black", size: "Free Size", fabric: "Microfiber", sku: "NMZ-TRV-002-BLK", stock: 35 },
        { color: "Cream", size: "Free Size", fabric: "Microfiber", sku: "NMZ-TRV-002-CRM", stock: 20 },
      ],
    },
    {
      name: "Padded Bonnet Prayer Set",
      slug: "padded-bonnet-prayer-set",
      description:
        "Extra-comfortable three-piece prayer set with a lightly padded bonnet that sits securely without pressure points — ideal for those with sensitive scalps or who pray for extended periods.\n\nComes with two large scarves in matching colours for complete coverage. Soft organic cotton blend that gets softer with every wash.\n\nCare: Machine wash gentle, cold. Lay flat to dry to maintain bonnet shape.",
      categoryId: bySlug["namaz-scarfs"],
      price: 79900,
      comparePrice: 99900,
      images: [us("photo-1602752250015-89e48e0f8d82", 800)],
      tags: ["namaz", "prayer", "padded", "comfort", "premium", "organic"],
      variants: [
        { color: "White", size: "Free Size", fabric: "Organic Cotton", sku: "NMZ-PAD-003-WHT", stock: 25 },
        { color: "Black", size: "Free Size", fabric: "Organic Cotton", sku: "NMZ-PAD-003-BLK", stock: 22 },
        { color: "Cream", size: "Free Size", fabric: "Organic Cotton", sku: "NMZ-PAD-003-CRM", stock: 18 },
      ],
    },

    /* ── ACCESSORIES ── */
    {
      name: "Magnetic Hijab Pins (Set of 6)",
      slug: "magnetic-hijab-pins",
      description:
        "Strong neodymium magnets in a sleek capsule casing — hold multiple layers of fabric firmly without snagging, piercing, or leaving marks. Safe for georgette, chiffon, jersey, and satin.\n\nSet of 6 in your choice of finish. Lightweight enough to forget you're wearing them. A staple in every hijabi's bag.\n\nNote: Keep away from phones, credit cards, and pacemakers.",
      categoryId: bySlug["accessories"],
      price: 29900,
      images: [us("photo-1576053139778-7e32f2ae3cfd")],
      tags: ["accessories", "pins", "magnetic", "set", "no-snag"],
      variants: [
        { color: "Matte Black",   sku: "ACC-PIN-001-BLK", stock: 60 },
        { color: "Rose Gold",     sku: "ACC-PIN-001-RGD", stock: 50 },
        { color: "Assorted",      sku: "ACC-PIN-001-AST", stock: 45 },
      ],
    },
    {
      name: "Cotton Underscarves (Pack of 3)",
      slug: "cotton-underscarves-pack",
      description:
        "The foundation of a great hijab — these snug underscarves keep your hijab in place, reduce friction, and provide an extra layer of modest coverage at the neckline.\n\nMade from soft, breathable ribbed cotton that hugs the head without causing headaches. The seamless toe-tube design means no uncomfortable ridges.\n\nCare: Machine wash cold. Do not bleach. Tumble dry low.",
      categoryId: bySlug["accessories"],
      price: 39900,
      comparePrice: 54900,
      images: [us("photo-1570172619644-dfd03ed5d881", 800)],
      tags: ["accessories", "underscarf", "bonnet", "cap", "pack", "cotton"],
      variants: [
        { color: "White",      size: "Free Size", sku: "ACC-UND-002-WHT", stock: 45 },
        { color: "Black",      size: "Free Size", sku: "ACC-UND-002-BLK", stock: 42 },
        { color: "Beige",      size: "Free Size", sku: "ACC-UND-002-BGE", stock: 38 },
        { color: "Assorted",   size: "Free Size", sku: "ACC-UND-002-AST", stock: 30 },
      ],
    },
    {
      name: "Hijab Styling Brooches (Set of 3)",
      slug: "hijab-styling-brooches",
      description:
        "Elevate a simple hijab into a statement look with these elegant brooches. Each set includes three designs — a classic floral, a geometric arabesque, and a pearl cluster — all in the same metal finish.\n\nSafe-lock clasps prevent accidental opening. The smooth back plate means no snagging on delicate fabrics. A thoughtful gift for any occasion.\n\nNote: Avoid contact with water to preserve the finish.",
      categoryId: bySlug["accessories"],
      price: 49900,
      comparePrice: 69900,
      images: [us("photo-1530023369754-d97a4b35f547", 800)],
      tags: ["accessories", "brooch", "styling", "gift", "decorative"],
      variants: [
        { color: "Gold Tone",   sku: "ACC-BRO-003-GLD", stock: 30 },
        { color: "Silver Tone", sku: "ACC-BRO-003-SLV", stock: 28 },
        { color: "Rose Gold",   sku: "ACC-BRO-003-RGD", stock: 22 },
      ],
    },
  ];

  /* ── UPSERT PRODUCTS + VARIANTS ─────────────────────────────────────────── */
  for (const p of products) {
    const { variants, ...data } = p;
    const product = await db.product.upsert({
      where: { slug: p.slug },
      create: { ...data, isActive: true },
      update: data,
    });
    for (const v of variants) {
      await db.productVariant.upsert({
        where: { sku: v.sku },
        create: { ...v, productId: product.id, isActive: true },
        update: { ...v, productId: product.id },
      });
    }
  }

  /* ── ANNOUNCEMENT BAR ───────────────────────────────────────────────────── */
  await db.announcementBar.upsert({
    where: { id: "seed-banner" },
    create: {
      id: "seed-banner",
      text: "✨ Free shipping on orders above ₹999 · Use code NAYABI10 for 10% off your first order",
      isActive: true,
    },
    update: {},
  });

  /* ── SAMPLE DISCOUNT ────────────────────────────────────────────────────── */
  await db.discountCode.upsert({
    where: { code: "NAYABI10" },
    create: {
      code: "NAYABI10",
      type: "PERCENT",
      value: 10,
      minOrderAmount: 50000,
      usageLimit: 1000,
      isActive: true,
    },
    update: {},
  });

  console.log("✓ Seeded: 1 admin, 4 categories, 16 products, 58 variants, announcement bar, discount code");
}

main()
  .catch((e) => { console.error(e); process.exit(1); })
  .finally(() => db.$disconnect());
