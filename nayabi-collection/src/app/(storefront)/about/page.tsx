import Image from "next/image";
import Link from "next/link";
import { GlassCard, GlassButton } from "@/components/ui";
import { Heart, Shield, Truck, Star, Leaf, Users } from "lucide-react";

export const metadata = {
  title: "About Us",
  description:
    "Nayabi Collection — premium modest wear for Muslim women across India. Learn about our story, values, and mission.",
};

const values = [
  {
    icon: Heart,
    title: "Faith-first design",
    description:
      "Every piece is created with modesty as the starting point — never an afterthought. Full coverage, opaque fabrics, and thoughtful cuts are non-negotiable.",
  },
  {
    icon: Shield,
    title: "Uncompromising quality",
    description:
      "We personally inspect every item before dispatch. Premium georgette, nida, chiffon, and crepe — sourced from the finest textile clusters in India.",
  },
  {
    icon: Truck,
    title: "Delivered with care",
    description:
      "Discreet, eco-friendly packaging and real-time tracking across all 28 states and 8 union territories. COD available nationwide.",
  },
  {
    icon: Star,
    title: "Customer first",
    description:
      "7-day hassle-free returns, free size exchanges, and a WhatsApp support team that responds within minutes.",
  },
  {
    icon: Leaf,
    title: "Sustainable choices",
    description:
      "We prioritise natural and low-impact fabrics, minimise packaging waste, and work with ethical manufacturers.",
  },
  {
    icon: Users,
    title: "Community rooted",
    description:
      "Built by and for Muslim women in India. A portion of every sale goes to supporting literacy programmes for women.",
  },
];

const teamMembers = [
  {
    name: "Ayesha Siddiqui",
    role: "Founder & Creative Director",
    bio: "A fashion designer with 12 years of experience who struggled to find modest wear that matched her style. Nayabi is the brand she always wanted to shop from.",
    image: "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=85",
  },
  {
    name: "Zainab Khan",
    role: "Head of Product & Sourcing",
    bio: "Travels to Surat, Varanasi, and Bhiwandi every season to hand-select fabrics and ensure every piece meets Nayabi's quality standards.",
    image: "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&w=400&q=85",
  },
];

export default function AboutPage() {
  return (
    <div className="mesh-bg min-h-screen">
      <div className="mx-auto max-w-5xl px-4 sm:px-6 py-16">

        {/* ── HERO ── */}
        <div className="text-center mb-14">
          <p className="section-label">Our story</p>
          <h1 className="mt-3 text-4xl sm:text-5xl text-[var(--color-text-primary)]">
            About{" "}
            <span className="gradient-text">Nayabi Collection</span>
          </h1>
          <p className="mt-5 max-w-2xl mx-auto text-[var(--color-text-secondary)] leading-relaxed">
            <em>Nayabi</em> — meaning &ldquo;rare&rdquo; or &ldquo;precious&rdquo; in Urdu — was born from a
            belief that modest fashion should be just as beautiful, considered, and joyful as any
            other form of self-expression.
          </p>
        </div>

        {/* ── STORY SECTION ── */}
        <div className="grid gap-8 lg:grid-cols-2 mb-12 items-center">
          <div className="relative aspect-[4/3] rounded-2xl overflow-hidden glass">
            <Image
              src="https://images.unsplash.com/photo-1583391733956-6c78276477e2?auto=format&fit=crop&w=800&q=85"
              alt="Nayabi Collection — modest wear crafted with care"
              fill
              className="object-cover"
              sizes="(max-width: 1024px) 100vw, 50vw"
            />
          </div>
          <div>
            <GlassCard padding="lg" className="h-full">
              <h2 className="text-2xl text-[var(--color-text-primary)] mb-4">
                Where it began
              </h2>
              <div className="space-y-4 text-sm text-[var(--color-text-secondary)] leading-relaxed">
                <p>
                  Nayabi Collection started in 2021 in Mumbai when our founder Ayesha found herself
                  spending hours searching for a prayer scarf that was both beautiful and practical —
                  and coming up empty. The market was flooded with synthetic, poorly finished pieces
                  that didn&apos;t honour the gravity of prayer, or overpriced imports that most
                  families couldn&apos;t afford.
                </p>
                <p>
                  She spent six months visiting textile markets across Surat, Bhiwandi, and Varanasi
                  to understand what &ldquo;premium modest wear&rdquo; could really mean when made in India,
                  for India. The result was a small first collection of 12 hijabs and 3 namaz scarfs
                  that sold out in 48 hours through Instagram.
                </p>
                <p>
                  Today, Nayabi serves over 10,000 customers across India — from Srinagar to
                  Thiruvananthapuram — and has expanded to include abayas, accessories, and seasonal
                  collections for Ramadan and Eid.
                </p>
              </div>
            </GlassCard>
          </div>
        </div>

        {/* ── VALUES ── */}
        <div className="mb-12">
          <div className="text-center mb-8">
            <p className="section-label">What drives us</p>
            <h2 className="mt-2 text-3xl text-[var(--color-text-primary)]">Our values</h2>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {values.map((v) => {
              const Icon = v.icon;
              return (
                <GlassCard key={v.title} padding="md">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[var(--color-gold)]/15 mb-3">
                    <Icon className="h-5 w-5 text-[var(--color-gold)]" aria-hidden="true" />
                  </div>
                  <h3 className="text-sm font-semibold text-[var(--color-text-primary)] mb-1">
                    {v.title}
                  </h3>
                  <p className="text-xs text-[var(--color-text-secondary)] leading-relaxed">
                    {v.description}
                  </p>
                </GlassCard>
              );
            })}
          </div>
        </div>

        {/* ── TEAM ── */}
        <div className="mb-12">
          <div className="text-center mb-8">
            <p className="section-label">The people behind Nayabi</p>
            <h2 className="mt-2 text-3xl text-[var(--color-text-primary)]">Our team</h2>
          </div>
          <div className="grid gap-6 sm:grid-cols-2">
            {teamMembers.map((m) => (
              <GlassCard key={m.name} padding="md" className="flex gap-4">
                <div className="relative h-16 w-16 shrink-0 rounded-full overflow-hidden bg-[var(--color-bg-mid)]">
                  <Image src={m.image} alt={m.name} fill className="object-cover" sizes="64px" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-[var(--color-text-primary)]">{m.name}</p>
                  <p className="text-xs text-[var(--color-gold)] mb-2">{m.role}</p>
                  <p className="text-xs text-[var(--color-text-secondary)] leading-relaxed">{m.bio}</p>
                </div>
              </GlassCard>
            ))}
          </div>
        </div>

        {/* ── STATS ── */}
        <GlassCard tier="elevated" padding="lg" className="mb-10 text-center">
          <div className="grid grid-cols-2 gap-6 sm:grid-cols-4">
            {[
              { value: "10,000+", label: "Happy customers" },
              { value: "4.9★", label: "Average rating" },
              { value: "500+", label: "Products curated" },
              { value: "28+", label: "States served" },
            ].map((s) => (
              <div key={s.label}>
                <p className="text-3xl font-semibold text-[var(--color-gold)]">{s.value}</p>
                <p className="mt-1 text-xs text-[var(--color-text-muted)] uppercase tracking-widest">{s.label}</p>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* ── CTA ── */}
        <div className="text-center flex flex-col sm:flex-row gap-4 justify-center">
          <Link href="/shop">
            <GlassButton size="lg">Shop the collection</GlassButton>
          </Link>
          <Link href="/contact">
            <GlassButton size="lg" variant="secondary">Contact us</GlassButton>
          </Link>
        </div>

      </div>
    </div>
  );
}
