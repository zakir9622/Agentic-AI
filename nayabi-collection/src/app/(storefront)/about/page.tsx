import { GlassCard } from "@/components/ui";
import { Heart, Shield, Truck, Star } from "lucide-react";

export const metadata = {
  title: "About Nayabi Collection",
  description:
    "Nayabi Collection brings beautifully crafted modest wear — hijabs, abayas, and prayer scarfs — to Muslim women across India.",
};

const values = [
  {
    icon: Heart,
    title: "Made with care",
    description:
      "Every piece is thoughtfully designed to balance faith, comfort, and contemporary style.",
  },
  {
    icon: Shield,
    title: "Quality assured",
    description:
      "We source premium fabrics and rigorously check each item before it reaches you.",
  },
  {
    icon: Truck,
    title: "Delivered to your door",
    description:
      "Fast, reliable shipping across India with real-time tracking on every order.",
  },
  {
    icon: Star,
    title: "Customer first",
    description:
      "7-day hassle-free returns and a dedicated support team ready to help.",
  },
];

export default function AboutPage() {
  return (
    <div className="mesh-bg min-h-screen">
      <div className="mx-auto max-w-4xl px-4 sm:px-6 py-16">
        {/* Hero */}
        <GlassCard padding="lg" className="mb-10 text-center">
          <h1 className="text-4xl text-[var(--color-text-primary)]">
            About{" "}
            <span className="gradient-text">Nayabi Collection</span>
          </h1>
          <p className="mt-4 max-w-2xl mx-auto text-[var(--color-text-secondary)] leading-relaxed">
            Nayabi &mdash; meaning &ldquo;rare&rdquo; in Urdu &mdash; was born from a simple belief: modest fashion
            should never compromise beauty or comfort. We curate and craft hijabs, abayas,
            and prayer scarfs for Muslim women across India who want to express their faith
            with grace.
          </p>
        </GlassCard>

        {/* Story */}
        <GlassCard padding="lg" className="mb-8">
          <h2 className="text-2xl text-[var(--color-text-primary)] mb-4">Our story</h2>
          <div className="space-y-4 text-[var(--color-text-secondary)] leading-relaxed">
            <p>
              Nayabi Collection started with a mother struggling to find quality prayer scarfs
              for her daughter. What began as a small search turned into a mission: to bring
              thoughtfully designed modest wear directly to women who deserve the best.
            </p>
            <p>
              Today we serve thousands of customers across India, offering a carefully curated
              range of hijabs in premium georgette, chiffon, and jersey fabrics; elegant abayas
              for everyday and special occasions; and lightweight namaz scarfs for comfortable
              prayer.
            </p>
            <p>
              Every product is selected with our community in mind — wearable, washable, and
              worthy of the occasion.
            </p>
          </div>
        </GlassCard>

        {/* Values */}
        <div className="grid gap-4 sm:grid-cols-2 mb-8">
          {values.map((v) => {
            const Icon = v.icon;
            return (
              <GlassCard key={v.title} padding="md">
                <div className="flex items-start gap-3">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[var(--color-gold)]/15">
                    <Icon className="h-5 w-5 text-[var(--color-gold)]" aria-hidden="true" />
                  </div>
                  <div>
                    <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
                      {v.title}
                    </h3>
                    <p className="mt-1 text-xs text-[var(--color-text-secondary)] leading-relaxed">
                      {v.description}
                    </p>
                  </div>
                </div>
              </GlassCard>
            );
          })}
        </div>

        {/* CTA */}
        <GlassCard padding="md" className="text-center">
          <p className="text-sm text-[var(--color-text-secondary)]">
            Have questions?{" "}
            <a
              href="/contact"
              className="text-[var(--color-gold)] underline underline-offset-2"
            >
              We&apos;d love to hear from you.
            </a>
          </p>
        </GlassCard>
      </div>
    </div>
  );
}
