import type { Metadata, Viewport } from "next";
import { Inter, Playfair_Display } from "next/font/google";
import { ToastContainer } from "@/components/ui";
import { Providers } from "./providers";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

const playfair = Playfair_Display({
  subsets: ["latin"],
  variable: "--font-playfair",
  display: "swap",
  weight: ["400", "500", "600", "700"],
  style: ["normal", "italic"],
});

export const metadata: Metadata = {
  title: {
    default: "Nayabi Collection — Premium Modest Wear",
    template: "%s | Nayabi Collection",
  },
  description:
    "Discover beautifully crafted Hijabs, Abayas, and Namaz Scarfs. Premium modest fashion delivered across India.",
  keywords: ["hijab", "abaya", "namaz scarf", "modest wear", "islamic fashion", "india"],
  authors: [{ name: "Nayabi Collection" }],
  creator: "Nayabi Collection",
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000"
  ),
  openGraph: {
    type: "website",
    siteName: "Nayabi Collection",
    title: "Nayabi Collection — Premium Modest Wear",
    description: "Discover beautifully crafted Hijabs, Abayas, and Namaz Scarfs.",
    locale: "en_IN",
  },
  twitter: {
    card: "summary_large_image",
    title: "Nayabi Collection",
    description: "Premium modest wear delivered across India.",
  },
  robots: { index: true, follow: true },
  icons: {
    icon: "/favicon.ico",
    apple: "/apple-touch-icon.png",
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#0A0A14",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang="en-IN"
      suppressHydrationWarning
      className={`${inter.variable} ${playfair.variable}`}
      style={{
        ["--font-body" as string]:    "var(--font-inter), system-ui, sans-serif",
        ["--font-display" as string]: "var(--font-playfair), Georgia, serif",
      }}
    >
      <body className="min-h-dvh flex flex-col antialiased">
        <Providers>
          {children}
          <ToastContainer />
        </Providers>
      </body>
    </html>
  );
}
