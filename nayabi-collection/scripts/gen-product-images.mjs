/**
 * Generates on-brand SVG product images into public/products/ so the demo
 * storefront always shows real, recognizable garment visuals (hijabs, abayas,
 * scarfs, accessories) without depending on a remote image host. Swap these
 * for photographs by replacing the files (keep the same names) or by seeding
 * the database with real product image URLs.
 *
 * Run: node scripts/gen-product-images.mjs
 */
import { writeFileSync, mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const OUT = join(dirname(fileURLToPath(import.meta.url)), "..", "public", "products");
mkdirSync(OUT, { recursive: true });

// ── palette helpers ─────────────────────────────────────────────────────────
// Each product gets a studio backdrop (bgTop→bgBot), a garment colour pair
// (light highlight → deep shadow) and a gold-ish accent.
const W = 480;
const H = 600;

function head() {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}" role="img">`;
}

function defs(id, c) {
  return `<defs>
    <linearGradient id="bg-${id}" x1="0" y1="0" x2="0.4" y2="1">
      <stop offset="0%" stop-color="${c.bgTop}"/>
      <stop offset="100%" stop-color="${c.bgBot}"/>
    </linearGradient>
    <radialGradient id="glow-${id}" cx="28%" cy="20%" r="75%">
      <stop offset="0%" stop-color="#ffffff" stop-opacity="0.55"/>
      <stop offset="55%" stop-color="#ffffff" stop-opacity="0.10"/>
      <stop offset="100%" stop-color="#ffffff" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="cloth-${id}" x1="0.15" y1="0.1" x2="0.85" y2="0.95">
      <stop offset="0%" stop-color="${c.light}"/>
      <stop offset="55%" stop-color="${c.mid}"/>
      <stop offset="100%" stop-color="${c.deep}"/>
    </linearGradient>
    <radialGradient id="floor-${id}" cx="50%" cy="100%" r="60%">
      <stop offset="0%" stop-color="#000000" stop-opacity="0.18"/>
      <stop offset="100%" stop-color="#000000" stop-opacity="0"/>
    </radialGradient>
  </defs>`;
}

function backdrop(id) {
  return `<rect width="${W}" height="${H}" fill="url(#bg-${id})"/>
    <rect width="${W}" height="${H}" fill="url(#glow-${id})"/>
    <ellipse cx="${W / 2}" cy="${H - 40}" rx="190" ry="46" fill="url(#floor-${id})"/>`;
}

function frame(c) {
  // subtle gold inset frame + category tag
  return `<rect x="14" y="14" width="${W - 28}" height="${H - 28}" rx="22" fill="none" stroke="${c.accent}" stroke-opacity="0.45" stroke-width="1.5"/>`;
}

function tag(c, label) {
  return `<g>
    <rect x="28" y="28" rx="13" width="${28 + label.length * 8.4}" height="26" fill="#000000" fill-opacity="0.18"/>
    <text x="42" y="45" font-family="Georgia, serif" font-size="13" letter-spacing="2" fill="${c.accent}">${label.toUpperCase()}</text>
  </g>`;
}

function sparkle(x, y, s, c) {
  return `<path transform="translate(${x},${y}) scale(${s})" d="M0,-10 L2.4,-2.4 10,0 2.4,2.4 0,10 -2.4,2.4 -10,0 -2.4,-2.4 Z" fill="${c.accent}" fill-opacity="0.9"/>`;
}

// ── garment silhouettes ─────────────────────────────────────────────────────
function hijab(id, c) {
  // face oval framed by a draped head-scarf flowing to the shoulders
  return `
    <ellipse cx="240" cy="250" rx="62" ry="78" fill="${c.skin}"/>
    <path fill="url(#cloth-${id})" fill-rule="evenodd" d="
      M240,96
      C312,96 352,150 360,224
      C368,316 388,430 398,520
      L398,560 L82,560 L82,520
      C92,430 112,316 120,224
      C128,150 168,96 240,96 Z
      M240,178
      C206,178 184,210 184,252
      C184,300 208,330 240,330
      C272,330 296,300 296,252
      C296,210 274,178 240,178 Z" />
    <path d="M240,150 C300,150 322,206 320,250" fill="none" stroke="${c.deep}" stroke-opacity="0.35" stroke-width="6" stroke-linecap="round"/>
    <path d="M150,300 C160,400 150,480 138,540" fill="none" stroke="${c.deep}" stroke-opacity="0.30" stroke-width="7" stroke-linecap="round"/>
    <path d="M330,300 C322,400 332,480 344,540" fill="none" stroke="${c.light}" stroke-opacity="0.40" stroke-width="6" stroke-linecap="round"/>`;
}

function abaya(id, c) {
  // flowing full-length robe: shoulders → flared hem with sleeves + centre seam
  return `
    <path fill="url(#cloth-${id})" d="
      M240,120
      C214,120 198,128 190,150
      L150,196 C120,214 112,250 124,300 L156,300
      C146,372 124,470 112,556 L368,556
      C356,470 334,372 324,300 L356,300
      C368,250 360,214 330,196 L290,150
      C282,128 266,120 240,120 Z" />
    <path d="M240,120 C232,150 232,150 240,176 C248,150 248,150 240,120 Z" fill="${c.deep}" fill-opacity="0.5"/>
    <path d="M240,182 L240,548" stroke="${c.deep}" stroke-opacity="0.32" stroke-width="4"/>
    <path d="M196,210 C186,330 176,450 172,548" fill="none" stroke="${c.light}" stroke-opacity="0.35" stroke-width="6" stroke-linecap="round"/>
    <path d="M286,210 C296,330 306,450 310,548" fill="none" stroke="${c.deep}" stroke-opacity="0.28" stroke-width="6" stroke-linecap="round"/>
    <path d="M150,210 C140,250 138,276 150,298" fill="none" stroke="${c.deep}" stroke-opacity="0.25" stroke-width="5"/>`;
}

function scarf(id, c) {
  // neatly folded prayer-scarf stack with diagonal fold lines
  return `
    <g transform="translate(0,18)">
      <rect x="118" y="300" width="244" height="150" rx="20" fill="${c.deep}" fill-opacity="0.55"/>
      <rect x="104" y="250" width="272" height="150" rx="22" fill="url(#cloth-${id})"/>
      <rect x="120" y="206" width="240" height="120" rx="22" fill="url(#cloth-${id})"/>
      <path d="M120,250 L360,210" stroke="${c.light}" stroke-opacity="0.45" stroke-width="3"/>
      <path d="M104,330 L376,300" stroke="${c.deep}" stroke-opacity="0.30" stroke-width="3"/>
      <path d="M150,206 C150,150 200,120 240,120 C280,120 330,150 330,206" fill="none" stroke="${c.mid}" stroke-width="26" stroke-linecap="round"/>
      <path d="M150,206 C150,150 200,120 240,120 C280,120 330,150 330,206" fill="none" stroke="${c.light}" stroke-opacity="0.4" stroke-width="6" stroke-linecap="round"/>
    </g>`;
}

function pins(id, c) {
  // a fan of satin hijab pins with rounded jewelled heads
  const stems = [];
  const angles = [-26, -13, 0, 13, 26];
  for (const a of angles) {
    stems.push(`<g transform="translate(240,360) rotate(${a})">
      <rect x="-3" y="-200" width="6" height="220" rx="3" fill="url(#cloth-${id})"/>
      <circle cx="0" cy="-204" r="20" fill="${c.light}"/>
      <circle cx="-6" cy="-210" r="6" fill="#ffffff" fill-opacity="0.7"/>
      <circle cx="0" cy="-204" r="20" fill="none" stroke="${c.deep}" stroke-opacity="0.4" stroke-width="2"/>
    </g>`);
  }
  return `<g>${stems.join("")}<ellipse cx="240" cy="372" rx="40" ry="16" fill="${c.deep}" fill-opacity="0.5"/></g>`;
}

const SHAPES = { hijab, abaya, scarf, pins };

function build({ id, type, label, c }) {
  return [
    head(),
    defs(id, c),
    backdrop(id),
    SHAPES[type](id, c),
    sparkle(388, 150, 1.1, c),
    frame(c),
    tag(c, label),
    `</svg>`,
  ].join("");
}

// ── product set (mirrors src/lib/demo-data.ts) ──────────────────────────────
const items = [
  // hijabs
  { file: "silk-georgette-hijab", type: "hijab", label: "Hijab", c: { bgTop: "#f6e7ec", bgBot: "#d9b3c2", light: "#f4c9d6", mid: "#d98ba2", deep: "#a85571", skin: "#e9d2c2", accent: "#b9874f" } },
  { file: "chiffon-pashmina-hijab", type: "hijab", label: "Hijab", c: { bgTop: "#ece8f6", bgBot: "#bcb0dd", light: "#cfc3ef", mid: "#9f8fc9", deep: "#6f5b9c", skin: "#e9d2c2", accent: "#b9874f" } },
  { file: "cotton-jersey-hijab", type: "hijab", label: "Hijab", c: { bgTop: "#f1ece4", bgBot: "#cdbba6", light: "#e3d4bd", mid: "#c2a983", deep: "#8f7958", skin: "#e9d2c2", accent: "#a8834a" } },
  { file: "embroidered-nida-hijab", type: "hijab", label: "Hijab", c: { bgTop: "#e2efed", bgBot: "#a7c9c2", light: "#bfe0d8", mid: "#6fb0a4", deep: "#3c7a6f", skin: "#e9d2c2", accent: "#b9874f" } },
  // abayas
  { file: "classic-black-abaya", type: "abaya", label: "Abaya", c: { bgTop: "#e7e6ee", bgBot: "#b9b8c6", light: "#5a5866", mid: "#34323d", deep: "#181820", accent: "#c9a96e" } },
  { file: "open-front-farasha-abaya", type: "abaya", label: "Abaya", c: { bgTop: "#efe6ee", bgBot: "#c7a9c2", light: "#c99cc0", mid: "#9a6b91", deep: "#5f3f5a", accent: "#c9a96e" } },
  { file: "crepe-butterfly-abaya", type: "abaya", label: "Abaya", c: { bgTop: "#e8eaf0", bgBot: "#a9b1c4", light: "#8b94a8", mid: "#525a6e", deep: "#2c3242", accent: "#c9a96e" } },
  { file: "embellished-occasion-abaya", type: "abaya", label: "Abaya", c: { bgTop: "#efe4ec", bgBot: "#b58aa0", light: "#9c5f78", mid: "#6f3a52", deep: "#431f31", accent: "#e2c48b" } },
  // namaz scarfs
  { file: "premium-prayer-scarf-set", type: "scarf", label: "Namaz Scarf", c: { bgTop: "#f5f1ea", bgBot: "#ddd2c0", light: "#f3ecdd", mid: "#e0d2b6", deep: "#b7a380", accent: "#a8834a" } },
  { file: "travel-fold-prayer-scarf", type: "scarf", label: "Namaz Scarf", c: { bgTop: "#e7edf3", bgBot: "#aac0d6", light: "#bcd2e6", mid: "#7fa3c2", deep: "#4d7196", accent: "#b9874f" } },
  { file: "embroidered-ramadan-scarf", type: "scarf", label: "Namaz Scarf", c: { bgTop: "#e4efe6", bgBot: "#a6c7ad", light: "#bfe0c5", mid: "#6fae7c", deep: "#3c7a4c", accent: "#c9a96e" } },
  // accessories
  { file: "satin-hijab-pin-set", type: "pins", label: "Accessory", c: { bgTop: "#f4ece0", bgBot: "#d9c39a", light: "#e8cf95", mid: "#c9a96e", deep: "#977a45", accent: "#7c5c1e" } },
];

// categories
const cats = [
  { file: "cat-hijabs", type: "hijab", label: "Hijabs", c: { bgTop: "#f6e7ec", bgBot: "#cf9fb3", light: "#f4c9d6", mid: "#d98ba2", deep: "#a85571", skin: "#e9d2c2", accent: "#b9874f" } },
  { file: "cat-abayas", type: "abaya", label: "Abayas", c: { bgTop: "#ece6f1", bgBot: "#9a7fa8", light: "#6a5072", mid: "#3f2f47", deep: "#1f1626", accent: "#c9a96e" } },
  { file: "cat-namaz-scarfs", type: "scarf", label: "Namaz Scarfs", c: { bgTop: "#f5f1ea", bgBot: "#cdbfa3", light: "#f3ecdd", mid: "#e0d2b6", deep: "#b7a380", accent: "#a8834a" } },
  { file: "cat-accessories", type: "pins", label: "Accessories", c: { bgTop: "#f4ece0", bgBot: "#d2b985", light: "#e8cf95", mid: "#c9a96e", deep: "#977a45", accent: "#7c5c1e" } },
];

let n = 0;
for (const it of [...items, ...cats]) {
  const svg = build({ id: it.file, type: it.type, label: it.label, c: it.c });
  writeFileSync(join(OUT, `${it.file}.svg`), svg, "utf8");
  n++;
}
console.log(`generated ${n} product/category SVGs in public/products/`);
