import { GlassCard } from "@/components/ui";

export const metadata = {
  title: "Size Guide",
  description: "Find your perfect fit with the Nayabi Collection size guide.",
};

const abayas = [
  { size: "S", chest: "88–92", waist: "70–74", length: "145" },
  { size: "M", chest: "92–96", waist: "74–78", length: "147" },
  { size: "L", chest: "96–102", waist: "78–84", length: "149" },
  { size: "XL", chest: "102–108", waist: "84–90", length: "151" },
  { size: "2XL", chest: "108–116", waist: "90–98", length: "153" },
  { size: "3XL", chest: "116–124", waist: "98–106", length: "155" },
];

export default function SizeGuidePage() {
  return (
    <div className="mesh-bg min-h-screen">
      <div className="mx-auto max-w-3xl px-4 sm:px-6 py-16">
        <div className="mb-10 text-center">
          <h1 className="text-4xl text-[var(--color-text-primary)]">
            Size <span className="text-[var(--color-gold)]">Guide</span>
          </h1>
          <p className="mt-3 text-[var(--color-text-secondary)] text-sm">
            All measurements in centimetres (cm). When between sizes, size up.
          </p>
        </div>

        {/* Hijabs */}
        <GlassCard padding="lg" className="mb-6">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)] mb-4">Hijabs</h2>
          <p className="text-sm text-[var(--color-text-secondary)] leading-relaxed mb-4">
            Most of our hijabs are <strong className="text-[var(--color-text-primary)]">one size fits all</strong> and
            are designed to comfortably fit head circumferences of 52–60 cm.
          </p>
          <div className="grid gap-3 sm:grid-cols-3">
            {[
              { label: "Square hijabs", value: "110 × 110 cm" },
              { label: "Rectangular hijabs", value: "70 × 180 cm" },
              { label: "Shayla hijabs", value: "75 × 200 cm" },
            ].map((item) => (
              <div
                key={item.label}
                className="rounded-lg border border-[var(--color-glass-border)] p-3 text-center"
              >
                <p className="text-xs text-[var(--color-text-muted)]">{item.label}</p>
                <p className="text-sm font-semibold text-[var(--color-gold)] mt-1">{item.value}</p>
              </div>
            ))}
          </div>
        </GlassCard>

        {/* Abayas */}
        <GlassCard padding="lg" className="mb-6">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)] mb-4">Abayas</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm" aria-label="Abaya size chart">
              <thead>
                <tr className="border-b border-[var(--color-glass-border)]">
                  <th scope="col" className="py-2 pr-4 text-left text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                    Size
                  </th>
                  <th scope="col" className="py-2 pr-4 text-center text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                    Chest (cm)
                  </th>
                  <th scope="col" className="py-2 pr-4 text-center text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                    Waist (cm)
                  </th>
                  <th scope="col" className="py-2 text-center text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                    Length (cm)
                  </th>
                </tr>
              </thead>
              <tbody>
                {abayas.map((row, idx) => (
                  <tr
                    key={row.size}
                    className={`border-b border-[var(--color-glass-border)] last:border-0 ${idx % 2 === 0 ? "" : "bg-[var(--color-glass-bg)]"}`}
                  >
                    <td className="py-2.5 pr-4 font-semibold text-[var(--color-gold)]">
                      {row.size}
                    </td>
                    <td className="py-2.5 pr-4 text-center text-[var(--color-text-secondary)]">
                      {row.chest}
                    </td>
                    <td className="py-2.5 pr-4 text-center text-[var(--color-text-secondary)]">
                      {row.waist}
                    </td>
                    <td className="py-2.5 text-center text-[var(--color-text-secondary)]">
                      {row.length}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </GlassCard>

        {/* Namaz scarfs */}
        <GlassCard padding="lg" className="mb-6">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)] mb-4">
            Namaz / Prayer Scarfs
          </h2>
          <p className="text-sm text-[var(--color-text-secondary)] leading-relaxed">
            Our prayer scarfs are designed to provide full coverage during namaz.
            They are one size fits all, approximately{" "}
            <strong className="text-[var(--color-text-primary)]">120 × 120 cm</strong>.
          </p>
        </GlassCard>

        {/* How to measure */}
        <GlassCard padding="md">
          <h2 className="text-sm font-semibold text-[var(--color-text-primary)] mb-3">
            How to measure yourself
          </h2>
          <ul className="space-y-2 text-sm text-[var(--color-text-secondary)]">
            <li>
              <strong className="text-[var(--color-text-primary)]">Chest:</strong> Measure around
              the fullest part of your chest, keeping the tape parallel to the ground.
            </li>
            <li>
              <strong className="text-[var(--color-text-primary)]">Waist:</strong> Measure around
              your natural waistline, the narrowest part of your torso.
            </li>
            <li>
              <strong className="text-[var(--color-text-primary)]">Length:</strong> Measured from
              top shoulder to hem.
            </li>
          </ul>
          <p className="mt-3 text-xs text-[var(--color-text-muted)]">
            Still unsure? Contact us at{" "}
            <a
              href="mailto:support@nayabicollection.com"
              className="text-[var(--color-gold)] underline underline-offset-2"
            >
              support@nayabicollection.com
            </a>{" "}
            and we&apos;ll help you choose the right size.
          </p>
        </GlassCard>
      </div>
    </div>
  );
}
