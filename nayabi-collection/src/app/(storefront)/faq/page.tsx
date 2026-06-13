"use client";

import * as React from "react";
import { GlassCard } from "@/components/ui";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

const faqs = [
  {
    category: "Orders",
    items: [
      {
        q: "How do I track my order?",
        a: "Once your order ships, you'll receive a tracking link via SMS and WhatsApp. You can also track it from My Account → Orders.",
      },
      {
        q: "Can I cancel or modify my order?",
        a: "Orders can be cancelled or modified within 1 hour of placement. After that, please contact us immediately at support@nayabicollection.com.",
      },
      {
        q: "What payment methods do you accept?",
        a: "We accept UPI (Google Pay, PhonePe, Paytm), debit/credit cards, net banking, wallets, and Cash on Delivery (COD). COD has a ₹50 convenience fee.",
      },
    ],
  },
  {
    category: "Shipping",
    items: [
      {
        q: "Do you ship across India?",
        a: "Yes, we ship to all pincodes across India. Delivery takes 3–8 business days depending on your location.",
      },
      {
        q: "Is there free shipping?",
        a: "Yes! Orders above ₹999 qualify for free shipping. Standard shipping for orders below ₹999 is ₹79.",
      },
      {
        q: "Do you ship internationally?",
        a: "Not yet, but we plan to soon. Subscribe to our newsletter to be the first to know.",
      },
    ],
  },
  {
    category: "Returns & Exchanges",
    items: [
      {
        q: "What is your return policy?",
        a: "We offer 7-day hassle-free returns on unused, unwashed items with tags intact. Visit My Account → Orders to initiate a return.",
      },
      {
        q: "How long does a refund take?",
        a: "Refunds are processed within 5–7 business days after we receive the returned item.",
      },
      {
        q: "Can I exchange for a different size or color?",
        a: "Absolutely. Select 'Exchange' when initiating a return request and we'll ship the new item once we receive the original.",
      },
    ],
  },
  {
    category: "Products",
    items: [
      {
        q: "How do I choose the right size?",
        a: "Check our Size Guide page for detailed measurements. If unsure, our hijabs are usually one-size-fits-all and our abayas come in S–3XL.",
      },
      {
        q: "How do I care for my hijab or abaya?",
        a: "Machine wash on gentle cycle with cold water. Do not bleach. Air dry or tumble dry on low. Iron on low heat if needed.",
      },
      {
        q: "Are your products halal / ethically made?",
        a: "Yes, we source from ethical manufacturers and ensure our products meet modest wear standards.",
      },
    ],
  },
];

function AccordionItem({ q, a }: { q: string; a: string }) {
  const [open, setOpen] = React.useState(false);
  return (
    <div className="border-b border-[var(--color-glass-border)] last:border-0">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="flex w-full items-start justify-between gap-4 py-4 text-left"
      >
        <span className="text-sm font-medium text-[var(--color-text-primary)]">{q}</span>
        <ChevronDown
          className={cn(
            "h-4 w-4 shrink-0 text-[var(--color-text-muted)] transition-transform mt-0.5",
            open && "rotate-180"
          )}
          aria-hidden="true"
        />
      </button>
      {open && (
        <p className="pb-4 text-sm text-[var(--color-text-secondary)] leading-relaxed pr-8">
          {a}
        </p>
      )}
    </div>
  );
}

export default function FAQPage() {
  return (
    <div className="mesh-bg min-h-screen">
      <div className="mx-auto max-w-3xl px-4 sm:px-6 py-16">
        <div className="mb-10 text-center">
          <h1 className="text-4xl text-[var(--color-text-primary)]">
            Frequently Asked{" "}
            <span className="text-[var(--color-gold)]">Questions</span>
          </h1>
        </div>

        <div className="flex flex-col gap-6">
          {faqs.map((section) => (
            <GlassCard key={section.category} padding="md">
              <h2 className="text-base font-semibold text-[var(--color-gold)] mb-2">
                {section.category}
              </h2>
              <div>
                {section.items.map((item) => (
                  <AccordionItem key={item.q} q={item.q} a={item.a} />
                ))}
              </div>
            </GlassCard>
          ))}
        </div>

        <GlassCard padding="md" className="mt-6 text-center">
          <p className="text-sm text-[var(--color-text-secondary)]">
            Still have questions?{" "}
            <a
              href="/contact"
              className="text-[var(--color-gold)] underline underline-offset-2"
            >
              Contact us
            </a>
          </p>
        </GlassCard>
      </div>
    </div>
  );
}
